package com.eduquiz.feature.examroom.service;

import com.eduquiz.common.constant.ResponseCode;
import com.eduquiz.common.dto.ApiResponse;
import com.eduquiz.common.dto.PageResponse;
import com.eduquiz.common.exception.BadRequestException;
import com.eduquiz.common.exception.ResourceNotFoundException;
import com.eduquiz.common.util.RoomCodeGenerator;
import com.eduquiz.feature.auth.entity.User;
import com.eduquiz.feature.exam.entity.Exam;
import com.eduquiz.feature.exam.repository.ExamRepository;
import com.eduquiz.feature.examroom.dto.CreateRoomRequest;
import com.eduquiz.feature.examroom.dto.RoomResponse;
import com.eduquiz.feature.examroom.dto.RoomResultResponse;
import com.eduquiz.feature.examroom.entity.ExamRoom;
import com.eduquiz.feature.examroom.entity.RoomParticipant;
import com.eduquiz.feature.examroom.entity.RoomStatus;
import com.eduquiz.feature.examroom.repository.ExamRoomRepository;
import com.eduquiz.feature.examroom.repository.RoomParticipantRepository;
import com.eduquiz.feature.examsession.entity.ExamSession;
import com.eduquiz.feature.examsession.entity.SessionStatus;
import com.eduquiz.feature.examsession.repository.ExamSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExamRoomService {

    private final ExamRoomRepository examRoomRepository;
    private final RoomParticipantRepository participantRepository;
    private final ExamRepository examRepository;
    private final ExamSessionRepository examSessionRepository;

    // ══════════════════════════════════════════════════════
    // CREATE ROOM (TEACHER)
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<RoomResponse> createRoom(CreateRoomRequest request, User teacher) {
        log.info("[ExamRoomService.createRoom] START - examId={}, teacher={}", request.getExamId(), teacher.getEmail());

        // Validate exam exists and teacher has access (owner OR shared)
        Exam exam = examRepository.findByIdAndIsActiveTrue(request.getExamId())
                .orElseThrow(() -> new ResourceNotFoundException(ResponseCode.EXAM_NOT_FOUND));

        boolean isOwner = exam.getCreator().getId().equals(teacher.getId());
        boolean isShared = Boolean.TRUE.equals(exam.getIsShared());
        if (!isOwner && !isShared) {
            throw new BadRequestException(ResponseCode.EXAM_NOT_FOUND, "Bạn không có quyền sử dụng đề thi này.");
        }

        // Validate time
        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new BadRequestException(ResponseCode.BAD_REQUEST, "Thời gian kết thúc phải sau thời gian bắt đầu.");
        }

        // Generate unique room code
        String roomCode = generateUniqueRoomCode();

        ExamRoom room = ExamRoom.builder()
                .title(request.getTitle())
                .exam(exam)
                .teacher(teacher)
                .roomCode(roomCode)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .maxStudents(request.getMaxStudents())
                .build();

        examRoomRepository.save(room);

        log.info("[ExamRoomService.createRoom] SUCCESS - roomId={}, roomCode={}", room.getId(), roomCode);
        return ApiResponse.ok(ResponseCode.ROOM_CREATED_SUCCESS, toResponse(room, 0, 0));
    }

    // ══════════════════════════════════════════════════════
    // GET MY ROOMS (TEACHER)
    // ══════════════════════════════════════════════════════

    public ApiResponse<PageResponse<RoomResponse>> getMyRooms(User teacher, Pageable pageable) {
        log.info("[ExamRoomService.getMyRooms] START - teacher={}", teacher.getEmail());

        Page<RoomResponse> page = examRoomRepository
                .findByTeacherOrderByCreatedAtDesc(teacher, pageable)
                .map(room -> {
                    long participantCount = participantRepository.countByRoom(room);
                    long submittedCount = examSessionRepository.findByRoomAndStatus(room, SessionStatus.GRADED).size();
                    return toResponse(room, participantCount, submittedCount);
                });

        log.info("[ExamRoomService.getMyRooms] SUCCESS - total={}", page.getTotalElements());
        return ApiResponse.ok(ResponseCode.SUCCESS, PageResponse.of(page));
    }

    // ══════════════════════════════════════════════════════
    // GET ROOM DETAIL (TEACHER)
    // ══════════════════════════════════════════════════════

    public ApiResponse<RoomResponse> getRoomDetail(UUID roomId, User teacher) {
        log.info("[ExamRoomService.getRoomDetail] START - roomId={}", roomId);

        ExamRoom room = findRoom(roomId);
        verifyRoomOwner(room, teacher);

        long participantCount = participantRepository.countByRoom(room);
        long submittedCount = examSessionRepository.findByRoomAndStatus(room, SessionStatus.GRADED).size();

        return ApiResponse.ok(ResponseCode.SUCCESS, toResponse(room, participantCount, submittedCount));
    }

    // ══════════════════════════════════════════════════════
    // UPDATE ROOM STATUS (TEACHER)
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<RoomResponse> updateRoomStatus(UUID roomId, RoomStatus newStatus, User teacher) {
        log.info("[ExamRoomService.updateRoomStatus] START - roomId={}, newStatus={}", roomId, newStatus);

        ExamRoom room = findRoom(roomId);
        verifyRoomOwner(room, teacher);
        validateStatusTransition(room.getStatus(), newStatus);

        room.setStatus(newStatus);
        examRoomRepository.save(room);

        long participantCount = participantRepository.countByRoom(room);
        long submittedCount = examSessionRepository.findByRoomAndStatus(room, SessionStatus.GRADED).size();

        log.info("[ExamRoomService.updateRoomStatus] SUCCESS - roomId={}, status={}", roomId, newStatus);
        return ApiResponse.ok(ResponseCode.ROOM_UPDATED_SUCCESS, toResponse(room, participantCount, submittedCount));
    }

    // ══════════════════════════════════════════════════════
    // JOIN ROOM (STUDENT)
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<RoomResponse> joinRoom(String roomCode, User student) {
        log.info("[ExamRoomService.joinRoom] START - roomCode={}, student={}", roomCode, student.getEmail());

        ExamRoom room = examRoomRepository.findByRoomCode(roomCode.toUpperCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException(ResponseCode.ROOM_NOT_FOUND));

        // Check status: allow join when SCHEDULED or OPEN
        if (room.getStatus() == RoomStatus.CLOSED) {
            throw new BadRequestException(ResponseCode.ROOM_ALREADY_CLOSED, "Phòng thi đã đóng.");
        }

        // Check if already joined
        if (participantRepository.existsByRoomAndUser(room, student)) {
            throw new BadRequestException(ResponseCode.ROOM_ALREADY_JOINED, "Bạn đã tham gia phòng thi này.");
        }

        // Check capacity
        if (room.getMaxStudents() != null) {
            long current = participantRepository.countByRoom(room);
            if (current >= room.getMaxStudents()) {
                throw new BadRequestException(ResponseCode.ROOM_FULL, "Phòng thi đã đầy.");
            }
        }

        // Add participant
        RoomParticipant participant = RoomParticipant.builder()
                .id(new RoomParticipant.RoomParticipantId(room.getId(), student.getId()))
                .room(room)
                .user(student)
                .exam(room.getExam())
                .build();
        participantRepository.save(participant);

        long participantCount = participantRepository.countByRoom(room);

        log.info("[ExamRoomService.joinRoom] SUCCESS - roomId={}, student={}", room.getId(), student.getEmail());
        return ApiResponse.ok(ResponseCode.ROOM_JOIN_SUCCESS, toResponse(room, participantCount, 0));
    }

    // ══════════════════════════════════════════════════════
    // CHECK ROOM STATUS (STUDENT - public)
    // ══════════════════════════════════════════════════════

    public ApiResponse<RoomResponse> checkRoomByCode(String roomCode) {
        ExamRoom room = examRoomRepository.findByRoomCode(roomCode.toUpperCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException(ResponseCode.ROOM_NOT_FOUND));

        long participantCount = participantRepository.countByRoom(room);
        return ApiResponse.ok(ResponseCode.SUCCESS, toResponse(room, participantCount, 0));
    }

    // ══════════════════════════════════════════════════════
    // GET ROOM RESULTS (TEACHER)
    // ══════════════════════════════════════════════════════

    public ApiResponse<RoomResultResponse> getRoomResults(UUID roomId, User teacher) {
        log.info("[ExamRoomService.getRoomResults] START - roomId={}", roomId);

        ExamRoom room = findRoom(roomId);
        verifyRoomOwner(room, teacher);

        List<ExamSession> sessions = examSessionRepository.findByRoom(room);
        List<RoomParticipant> participants = participantRepository.findByRoom(room);

        RoomResultResponse.RoomInfo roomInfo = RoomResultResponse.RoomInfo.builder()
                .roomId(room.getId())
                .roomCode(room.getRoomCode())
                .title(room.getTitle())
                .examTitle(room.getExam().getTitle())
                .subjectName(room.getExam().getSubject().getName())
                .totalQuestions(room.getExam().getTotalQuestions())
                .totalParticipants(participants.size())
                .submittedCount((int) sessions.stream().filter(s -> s.getStatus() != SessionStatus.IN_PROGRESS).count())
                .build();

        List<RoomResultResponse.ParticipantResult> results = participants.stream().map(p -> {
            ExamSession session = sessions.stream()
                    .filter(s -> s.getUser().getId().equals(p.getUser().getId()))
                    .findFirst()
                    .orElse(null);

            return RoomResultResponse.ParticipantResult.builder()
                    .userId(p.getUser().getId())
                    .studentName(p.getUser().getFullName())
                    .score(session != null ? session.getScore() : null)
                    .correctCount(null) // calculated in grading
                    .totalQuestions(room.getExam().getTotalQuestions())
                    .submittedAt(session != null ? session.getEndTime() : null)
                    .status(session != null ? session.getStatus().name() : "NOT_STARTED")
                    .build();
        }).toList();

        RoomResultResponse response = RoomResultResponse.builder()
                .roomInfo(roomInfo)
                .participants(results)
                .build();

        return ApiResponse.ok(ResponseCode.SUCCESS, response);
    }

    // ══════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════

    private ExamRoom findRoom(UUID roomId) {
        return examRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException(ResponseCode.ROOM_NOT_FOUND));
    }

    private void verifyRoomOwner(ExamRoom room, User teacher) {
        if (!room.getTeacher().getId().equals(teacher.getId())) {
            throw new BadRequestException(ResponseCode.ROOM_NOT_FOUND, "Phòng thi không tồn tại.");
        }
    }

    private void validateStatusTransition(RoomStatus current, RoomStatus target) {
        boolean valid = switch (current) {
            case SCHEDULED -> target == RoomStatus.OPEN || target == RoomStatus.CLOSED;
            case OPEN -> target == RoomStatus.CLOSED;
            case IN_PROGRESS -> target == RoomStatus.CLOSED;
            case CLOSED -> false;
        };
        if (!valid) {
            throw new BadRequestException(ResponseCode.ROOM_INVALID_STATUS_TRANSITION,
                    "Không thể chuyển từ " + current + " sang " + target);
        }
    }

    private String generateUniqueRoomCode() {
        String code;
        int attempts = 0;
        do {
            code = RoomCodeGenerator.generateRoomCode();
            attempts++;
            if (attempts > 100) {
                throw new RuntimeException("Unable to generate unique room code after 100 attempts");
            }
        } while (examRoomRepository.existsByRoomCode(code));
        return code;
    }

    private RoomResponse toResponse(ExamRoom room, long participantCount, long submittedCount) {
        return RoomResponse.builder()
                .id(room.getId())
                .roomCode(room.getRoomCode())
                .title(room.getTitle())
                .examId(room.getExam().getId())
                .examTitle(room.getExam().getTitle())
                .subjectName(room.getExam().getSubject().getName())
                .teacherName(room.getTeacher() != null ? room.getTeacher().getFullName() : null)
                .startTime(room.getStartTime())
                .endTime(room.getEndTime())
                .status(room.getStatus())
                .maxStudents(room.getMaxStudents())
                .durationMinutes(room.getExam().getDurationMinutes())
                .participantCount(participantCount)
                .submittedCount(submittedCount)
                .build();
    }
}
