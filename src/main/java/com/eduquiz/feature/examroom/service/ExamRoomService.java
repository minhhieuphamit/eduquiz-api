package com.eduquiz.feature.examroom.service;

import com.eduquiz.common.constant.ResponseCode;
import com.eduquiz.common.dto.ApiResponse;
import com.eduquiz.common.dto.PageResponse;
import com.eduquiz.common.exception.BadRequestException;
import com.eduquiz.common.exception.ResourceNotFoundException;
import com.eduquiz.feature.auth.entity.User;
import com.eduquiz.feature.exam.entity.Exam;
import com.eduquiz.feature.exam.entity.ExamType;
import com.eduquiz.feature.exam.repository.ExamQuestionRepository;
import com.eduquiz.feature.exam.repository.ExamRepository;
import com.eduquiz.feature.examroom.dto.CreateRoomRequest;
import com.eduquiz.feature.examroom.dto.JoinRoomRequest;
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

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExamRoomService {

    private static final String ROOM_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int ROOM_CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ExamRoomRepository roomRepository;
    private final RoomParticipantRepository participantRepository;
    private final ExamSessionRepository sessionRepository;
    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;

    // ══════════════════════════════════════════════════════
    // CREATE ROOM (TEACHER)
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<RoomResponse> createRoom(CreateRoomRequest request, User currentUser) {
        log.info("[ExamRoom.create] teacher={}, examId={}", currentUser.getId(), request.getExamId());

        Exam exam = examRepository.findByIdAndIsActiveTrue(request.getExamId())
                .orElseThrow(() -> new ResourceNotFoundException(ResponseCode.EXAM_NOT_FOUND));

        if (request.getStartTime().isAfter(request.getEndTime()) ||
                request.getStartTime().isEqual(request.getEndTime())) {
            throw new BadRequestException(ResponseCode.BAD_REQUEST, "End time must be after start time");
        }

        // Duration override is only meaningful for PRACTICE exams
        Integer durationMinutes = null;
        if (exam.getExamType() == ExamType.PRACTICE && request.getDurationMinutes() != null
                && request.getDurationMinutes() > 0) {
            durationMinutes = request.getDurationMinutes();
        }

        RoomStatus initialStatus = LocalDateTime.now().isBefore(request.getStartTime())
                ? RoomStatus.SCHEDULED
                : RoomStatus.OPEN;

        ExamRoom room = ExamRoom.builder()
                .title(request.getTitle().trim())
                .exam(exam)
                .createdBy(currentUser)
                .roomCode(generateUniqueRoomCode())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .maxStudents(request.getMaxStudents())
                .durationMinutes(durationMinutes)
                .status(initialStatus)
                .build();

        roomRepository.save(room);
        log.info("[ExamRoom.create] Created room={}, code={}", room.getId(), room.getRoomCode());

        return ApiResponse.ok(ResponseCode.ROOM_CREATED_SUCCESS, toResponse(room, 0, 0));
    }

    // ══════════════════════════════════════════════════════
    // GET MY ROOMS (TEACHER)
    // ══════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public ApiResponse<PageResponse<RoomResponse>> getMyRooms(User currentUser, Pageable pageable) {
        Page<RoomResponse> page = roomRepository
                .findByCreatedByOrderByCreatedAtDesc(currentUser, pageable)
                .map(room -> {
                    int participantCount = participantRepository.countByRoom(room);
                    int submittedCount = countSubmitted(room);
                    return toResponse(room, participantCount, submittedCount);
                });

        return ApiResponse.ok(ResponseCode.ROOM_UPDATED_SUCCESS, PageResponse.of(page));
    }

    // ══════════════════════════════════════════════════════
    // GET ROOM DETAIL (TEACHER/STUDENT)
    // ══════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public ApiResponse<RoomResponse> getRoomDetail(UUID roomId, User currentUser) {
        ExamRoom room = findRoom(roomId);
        checkRoomReadAccess(room, currentUser);
        int participantCount = participantRepository.countByRoom(room);
        int submittedCount = countSubmitted(room);
        return ApiResponse.ok(ResponseCode.ROOM_UPDATED_SUCCESS, toResponse(room, participantCount, submittedCount));
    }

    // ══════════════════════════════════════════════════════
    // CHECK ROOM BY CODE (public — for student waiting room)
    // ══════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public ApiResponse<RoomResponse> checkRoomByCode(String roomCode) {
        ExamRoom room = roomRepository.findByRoomCode(roomCode.toUpperCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException(ResponseCode.ROOM_NOT_FOUND));
        int participantCount = participantRepository.countByRoom(room);
        int submittedCount = countSubmitted(room);
        return ApiResponse.ok(ResponseCode.ROOM_UPDATED_SUCCESS, toResponse(room, participantCount, submittedCount));
    }

    // ══════════════════════════════════════════════════════
    // JOIN ROOM (STUDENT)
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<RoomResponse> joinRoom(JoinRoomRequest request, User currentUser) {
        String code = request.getRoomCode().toUpperCase().trim();
        ExamRoom room = roomRepository.findByRoomCode(code)
                .orElseThrow(() -> new ResourceNotFoundException(ResponseCode.ROOM_NOT_FOUND));

        if (room.getStatus() == RoomStatus.IN_PROGRESS) {
            throw new BadRequestException(ResponseCode.ROOM_IN_PROGRESS);
        }
        if (room.getStatus() == RoomStatus.CLOSED) {
            throw new BadRequestException(ResponseCode.ROOM_ALREADY_CLOSED);
        }

        // Idempotent: if already joined, return room info (don't throw error)
        RoomParticipant.RoomParticipantId pid =
                new RoomParticipant.RoomParticipantId(room.getId(), currentUser.getId());
        if (participantRepository.existsById(pid)) {
            int participantCount = participantRepository.countByRoom(room);
            int submittedCount = countSubmitted(room);
            return ApiResponse.ok(ResponseCode.ROOM_JOIN_SUCCESS, toResponse(room, participantCount, submittedCount));
        }

        // Check max students
        if (room.getMaxStudents() != null) {
            int currentCount = participantRepository.countByRoom(room);
            if (currentCount >= room.getMaxStudents()) {
                throw new BadRequestException(ResponseCode.ROOM_FULL);
            }
        }

        RoomParticipant participant = RoomParticipant.builder()
                .id(pid)
                .room(room)
                .user(currentUser)
                .exam(room.getExam())
                .build();
        participantRepository.save(participant);

        log.info("[ExamRoom.join] student={} joined room={}", currentUser.getId(), room.getId());

        int participantCount = participantRepository.countByRoom(room);
        int submittedCount = countSubmitted(room);
        return ApiResponse.ok(ResponseCode.ROOM_JOIN_SUCCESS, toResponse(room, participantCount, submittedCount));
    }

    // ══════════════════════════════════════════════════════
    // UPDATE ROOM STATUS (TEACHER)
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<RoomResponse> updateRoomStatus(UUID roomId, String newStatusStr,
                                                      LocalDateTime newEndTime, User currentUser) {
        ExamRoom room = findRoom(roomId);
        checkTeacherAccess(room, currentUser);

        RoomStatus newStatus;
        try {
            newStatus = RoomStatus.valueOf(newStatusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(ResponseCode.ROOM_INVALID_STATUS_TRANSITION,
                    "Unknown status: " + newStatusStr);
        }

        validateStatusTransition(room.getStatus(), newStatus);

        // When reopening a CLOSED room, require a new future endTime to prevent
        // the scheduler from immediately re-closing the room.
        if (room.getStatus() == RoomStatus.CLOSED && newStatus == RoomStatus.OPEN) {
            if (newEndTime == null || !newEndTime.isAfter(LocalDateTime.now())) {
                throw new BadRequestException(ResponseCode.BAD_REQUEST,
                        "A future endTime is required when reopening a closed room");
            }
            room.setEndTime(newEndTime);
        }

        room.setStatus(newStatus);
        roomRepository.save(room);

        log.info("[ExamRoom.updateStatus] room={} → {}", roomId, newStatus);

        int participantCount = participantRepository.countByRoom(room);
        int submittedCount = countSubmitted(room);
        return ApiResponse.ok(ResponseCode.ROOM_UPDATED_SUCCESS, toResponse(room, participantCount, submittedCount));
    }

    // ══════════════════════════════════════════════════════
    // GET ROOM RESULTS (TEACHER)
    // ══════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public ApiResponse<RoomResultResponse> getRoomResults(UUID roomId, User currentUser) {
        ExamRoom room = findRoom(roomId);
        checkTeacherAccess(room, currentUser);

        List<RoomParticipant> participants = participantRepository.findByRoom(room);
        List<ExamSession> sessions = sessionRepository.findByRoom(room);

        int totalQuestions = examQuestionRepository.findByExam(room.getExam()).size();

        // Group all sessions by userId (sorted oldest → newest per user)
        Map<UUID, List<ExamSession>> sessionsByUser = sessions.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getUser().getId(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.stream()
                                        .sorted(Comparator.comparing(ExamSession::getStartTime))
                                        .collect(Collectors.toList())
                        )
                ));

        Set<SessionStatus> submittedStatuses = Set.of(
                SessionStatus.SUBMITTED, SessionStatus.AUTO_SUBMITTED);

        List<RoomResultResponse.ParticipantResult> results = participants.stream()
                .map(p -> {
                    UUID userId = p.getUser().getId();
                    List<ExamSession> userSessions = sessionsByUser.getOrDefault(userId, List.of());

                    List<RoomResultResponse.AttemptResult> attempts;
                    if (userSessions.isEmpty()) {
                        // No session yet — single WAITING entry
                        attempts = List.of(RoomResultResponse.AttemptResult.builder()
                                .attemptNumber(1)
                                .status("WAITING")
                                .totalQuestions(totalQuestions)
                                .build());
                    } else {
                        int[] counter = {0};
                        attempts = userSessions.stream()
                                .map(s -> {
                                    counter[0]++;
                                    String status;
                                    if (s.getStatus() == SessionStatus.IN_PROGRESS) {
                                        status = "STARTED";
                                    } else if (submittedStatuses.contains(s.getStatus())) {
                                        status = "SUBMITTED";
                                    } else {
                                        status = s.getStatus().name();
                                    }
                                    return RoomResultResponse.AttemptResult.builder()
                                            .sessionId(s.getId())
                                            .attemptNumber(counter[0])
                                            .status(status)
                                            .score(s.getScore())
                                            .correctCount(s.getCorrectCount())
                                            .totalQuestions(totalQuestions)
                                            .startedAt(s.getStartTime())
                                            .submittedAt(s.getSubmittedAt())
                                            .build();
                                })
                                .collect(Collectors.toList());
                    }

                    return RoomResultResponse.ParticipantResult.builder()
                            .userId(userId.toString())
                            .studentName(p.getUser().getFullName())
                            .attempts(attempts)
                            .build();
                })
                .collect(Collectors.toList());

        // A participant counts as "submitted" if they have at least one submitted attempt
        int submittedCount = (int) results.stream()
                .filter(r -> r.getAttempts().stream()
                        .anyMatch(a -> "SUBMITTED".equals(a.getStatus())))
                .count();

        RoomResultResponse.RoomInfo roomInfo = RoomResultResponse.RoomInfo.builder()
                .roomId(room.getId().toString())
                .roomCode(room.getRoomCode())
                .title(room.getTitle())
                .examTitle(room.getExam().getTitle())
                .subjectName(room.getExam().getSubject().getName())
                .status(room.getStatus())
                .totalQuestions(totalQuestions)
                .totalParticipants(participants.size())
                .submittedCount(submittedCount)
                .build();

        return ApiResponse.ok(ResponseCode.ROOM_UPDATED_SUCCESS,
                RoomResultResponse.builder()
                        .roomInfo(roomInfo)
                        .participants(results)
                        .build());
    }

    // ══════════════════════════════════════════════════════
    // PRIVATE: HELPERS
    // ══════════════════════════════════════════════════════

    private ExamRoom findRoom(UUID roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException(ResponseCode.ROOM_NOT_FOUND));
    }

    private void checkTeacherAccess(ExamRoom room, User currentUser) {
        String role = currentUser.getRole().getName();
        if ("ADMIN".equals(role)) return;
        if (!room.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new BadRequestException(ResponseCode.AUTH_FORBIDDEN);
        }
    }

    private void checkRoomReadAccess(ExamRoom room, User currentUser) {
        String role = currentUser.getRole().getName();
        if ("ADMIN".equals(role) || "TEACHER".equals(role)) {
            if ("TEACHER".equals(role) && !room.getCreatedBy().getId().equals(currentUser.getId())) {
                throw new BadRequestException(ResponseCode.AUTH_FORBIDDEN);
            }
            return;
        }
        // STUDENT: must be a participant
        RoomParticipant.RoomParticipantId pid =
                new RoomParticipant.RoomParticipantId(room.getId(), currentUser.getId());
        if (!participantRepository.existsById(pid)) {
            throw new BadRequestException(ResponseCode.AUTH_FORBIDDEN);
        }
    }

    private void validateStatusTransition(RoomStatus current, RoomStatus next) {
        boolean valid = switch (current) {
            case SCHEDULED -> next == RoomStatus.OPEN || next == RoomStatus.CLOSED;
            case OPEN      -> next == RoomStatus.IN_PROGRESS || next == RoomStatus.CLOSED;
            case IN_PROGRESS -> next == RoomStatus.CLOSED;
            case CLOSED    -> next == RoomStatus.OPEN;
        };
        if (!valid) {
            throw new BadRequestException(ResponseCode.ROOM_INVALID_STATUS_TRANSITION,
                    current + " → " + next + " is not allowed");
        }
    }

    private int countSubmitted(ExamRoom room) {
        return (int) sessionRepository.findByRoom(room).stream()
                .filter(s -> s.getStatus() == SessionStatus.SUBMITTED
                        || s.getStatus() == SessionStatus.AUTO_SUBMITTED)
                .count();
    }

    private String generateUniqueRoomCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(ROOM_CODE_LENGTH);
            for (int i = 0; i < ROOM_CODE_LENGTH; i++) {
                sb.append(ROOM_CODE_CHARS.charAt(RANDOM.nextInt(ROOM_CODE_CHARS.length())));
            }
            code = sb.toString();
        } while (roomRepository.existsByRoomCode(code));
        return code;
    }

    RoomResponse toResponse(ExamRoom room, int participantCount, int submittedCount) {
        int effectiveDuration = room.getDurationMinutes() != null
                ? room.getDurationMinutes()
                : room.getExam().getDurationMinutes();

        return RoomResponse.builder()
                .id(room.getId())
                .roomCode(room.getRoomCode())
                .title(room.getTitle())
                .examId(room.getExam().getId())
                .examTitle(room.getExam().getTitle())
                .subjectName(room.getExam().getSubject().getName())
                .teacherName(room.getCreatedBy().getFullName())
                .startTime(room.getStartTime())
                .endTime(room.getEndTime())
                .status(room.getStatus())
                .maxStudents(room.getMaxStudents())
                .durationMinutes(effectiveDuration)
                .participantCount(participantCount)
                .submittedCount(submittedCount)
                .build();
    }
}
