package com.eduquiz.feature.examsession.service;

import com.eduquiz.common.constant.ResponseCode;
import com.eduquiz.common.dto.ApiResponse;
import com.eduquiz.common.dto.PageResponse;
import com.eduquiz.common.exception.BadRequestException;
import com.eduquiz.common.exception.ResourceNotFoundException;
import com.eduquiz.feature.auth.entity.User;
import com.eduquiz.feature.exam.entity.Exam;
import com.eduquiz.feature.exam.entity.ExamQuestion;
import com.eduquiz.feature.exam.repository.ExamQuestionRepository;
import com.eduquiz.feature.exam.repository.ExamRepository;
import com.eduquiz.feature.examroom.entity.ExamRoom;
import com.eduquiz.feature.examroom.entity.RoomStatus;
import com.eduquiz.feature.examroom.repository.ExamRoomRepository;
import com.eduquiz.feature.examroom.repository.RoomParticipantRepository;
import com.eduquiz.feature.examsession.dto.*;
import com.eduquiz.feature.examsession.entity.ExamAnswer;
import com.eduquiz.feature.examsession.entity.ExamSession;
import com.eduquiz.feature.examsession.entity.SessionStatus;
import com.eduquiz.feature.examsession.repository.ExamAnswerRepository;
import com.eduquiz.feature.examsession.repository.ExamSessionRepository;
import com.eduquiz.feature.question.dto.OptionResponse;
import com.eduquiz.feature.question.entity.Question;
import com.eduquiz.feature.question.entity.QuestionOption;
import com.eduquiz.feature.question.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExamSessionService {

    private final ExamSessionRepository sessionRepository;
    private final ExamAnswerRepository answerRepository;
    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final ExamRoomRepository roomRepository;
    private final RoomParticipantRepository participantRepository;
    private final QuestionRepository questionRepository;

    // ══════════════════════════════════════════════════════
    // START EXAM
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<ExamSessionResponse> startExam(StartExamRequest request, User student) {
        log.info("[ExamSessionService.startExam] START - examId={}, roomId={}, student={}",
                request.getExamId(), request.getRoomId(), student.getEmail());

        Exam exam = examRepository.findByIdAndIsActiveTrue(request.getExamId())
                .orElseThrow(() -> new ResourceNotFoundException(ResponseCode.EXAM_NOT_FOUND));

        ExamRoom room = null;
        if (request.getRoomId() != null) {
            room = roomRepository.findById(request.getRoomId())
                    .orElseThrow(() -> new ResourceNotFoundException(ResponseCode.ROOM_NOT_FOUND));

            // Room must be OPEN
            if (room.getStatus() != RoomStatus.OPEN) {
                throw new BadRequestException(ResponseCode.ROOM_NOT_OPEN, "Phòng thi chưa mở hoặc đã đóng.");
            }

            // Student must be participant
            if (!participantRepository.existsByRoomAndUser(room, student)) {
                throw new BadRequestException(ResponseCode.ROOM_NOT_FOUND, "Bạn chưa tham gia phòng thi này.");
            }
        }

        // Check if session already exists
        Optional<ExamSession> existingSession = room != null
                ? sessionRepository.findByUserAndExamAndRoom(student, exam, room)
                : sessionRepository.findByUserAndExamAndRoomIsNull(student, exam);

        if (existingSession.isPresent()) {
            ExamSession existing = existingSession.get();
            if (existing.getStatus() == SessionStatus.IN_PROGRESS) {
                // Return existing session (resume)
                return ApiResponse.ok(ResponseCode.EXAM_SESSION_STARTED, toSessionResponse(existing));
            }
            throw new BadRequestException(ResponseCode.EXAM_SESSION_ALREADY_SUBMITTED, "Bạn đã nộp bài thi này.");
        }

        // Create session
        ExamSession session = ExamSession.builder()
                .user(student)
                .exam(exam)
                .room(room)
                .status(SessionStatus.IN_PROGRESS)
                .build();
        sessionRepository.save(session);

        // Create blank answers for all exam questions
        List<ExamQuestion> examQuestions = examQuestionRepository.findByExam(exam);
        List<ExamAnswer> answers = examQuestions.stream()
                .map(eq -> ExamAnswer.builder()
                        .session(session)
                        .question(eq.getQuestion())
                        .build())
                .toList();
        answerRepository.saveAll(answers);

        log.info("[ExamSessionService.startExam] SUCCESS - sessionId={}", session.getId());
        return ApiResponse.ok(ResponseCode.EXAM_SESSION_STARTED, toSessionResponse(session));
    }

    // ══════════════════════════════════════════════════════
    // SAVE ANSWER (auto-save per question)
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<Void> saveAnswer(UUID sessionId, AnswerRequest request, User student) {
        log.info("[ExamSessionService.saveAnswer] sessionId={}, questionId={}", sessionId, request.getQuestionId());

        ExamSession session = findSessionForStudent(sessionId, student);
        checkSessionActive(session);

        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException(ResponseCode.QUESTION_NOT_FOUND));

        ExamAnswer answer = answerRepository.findBySessionAndQuestion(session, question)
                .orElseThrow(() -> new BadRequestException(ResponseCode.EXAM_QUESTION_NOT_IN_SESSION,
                        "Câu hỏi không thuộc bài thi này."));

        answer.setAnswerContent(request.getSelectedAnswer());
        answerRepository.save(answer);

        return ApiResponse.ok(ResponseCode.EXAM_ANSWER_SAVED);
    }

    // ══════════════════════════════════════════════════════
    // SUBMIT EXAM
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<ExamSessionResponse> submitExam(UUID sessionId, User student) {
        log.info("[ExamSessionService.submitExam] START - sessionId={}", sessionId);

        ExamSession session = findSessionForStudent(sessionId, student);

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new BadRequestException(ResponseCode.EXAM_SESSION_ALREADY_SUBMITTED, "Bài thi đã được nộp.");
        }

        gradeAndSubmit(session);

        log.info("[ExamSessionService.submitExam] SUCCESS - sessionId={}, score={}", sessionId, session.getScore());
        return ApiResponse.ok(ResponseCode.EXAM_SUBMIT_SUCCESS, toSessionResponse(session));
    }

    // ══════════════════════════════════════════════════════
    // AUTO-SUBMIT (called by scheduler)
    // ══════════════════════════════════════════════════════

    @Transactional
    public void autoSubmitSession(ExamSession session) {
        if (session.getStatus() != SessionStatus.IN_PROGRESS) return;
        gradeAndSubmit(session);
        log.info("[ExamSessionService.autoSubmitSession] Auto-submitted sessionId={}", session.getId());
    }

    // ══════════════════════════════════════════════════════
    // GET RESULT
    // ══════════════════════════════════════════════════════

    public ApiResponse<ExamResultResponse> getResult(UUID sessionId, User student) {
        log.info("[ExamSessionService.getResult] sessionId={}", sessionId);

        ExamSession session = findSessionForStudent(sessionId, student);

        if (session.getStatus() == SessionStatus.IN_PROGRESS) {
            throw new BadRequestException(ResponseCode.EXAM_RESULT_NOT_READY, "Bài thi chưa được nộp.");
        }

        List<ExamAnswer> answers = answerRepository.findBySession(session);

        List<ExamResultResponse.AnswerDetail> details = answers.stream().map(ans -> {
            Question q = ans.getQuestion();
            List<QuestionOption> options = q.getOptions();

            String correctLabel = options.stream()
                    .filter(o -> Boolean.TRUE.equals(o.getIsCorrect()))
                    .map(QuestionOption::getLabel)
                    .findFirst()
                    .orElse("");

            return ExamResultResponse.AnswerDetail.builder()
                    .questionId(q.getId())
                    .content(q.getContent())
                    .optionA(getOptionContent(options, "A"))
                    .optionB(getOptionContent(options, "B"))
                    .optionC(getOptionContent(options, "C"))
                    .optionD(getOptionContent(options, "D"))
                    .selectedAnswer(ans.getAnswerContent())
                    .correctAnswer(correctLabel)
                    .isCorrect(Boolean.TRUE.equals(ans.getIsCorrect()))
                    .explanation(q.getExplanation())
                    .build();
        }).toList();

        int correctCount = (int) answers.stream().filter(a -> Boolean.TRUE.equals(a.getIsCorrect())).count();

        ExamResultResponse response = ExamResultResponse.builder()
                .sessionId(session.getId())
                .examTitle(session.getExam().getTitle())
                .subjectName(session.getExam().getSubject().getName())
                .score(session.getScore())
                .correctCount(correctCount)
                .totalQuestions(answers.size())
                .startedAt(session.getStartTime())
                .submittedAt(session.getEndTime())
                .answers(details)
                .build();

        return ApiResponse.ok(ResponseCode.EXAM_RESULT_FETCH_SUCCESS, response);
    }

    // ══════════════════════════════════════════════════════
    // GET HISTORY (paginated)
    // ══════════════════════════════════════════════════════

    public ApiResponse<PageResponse<ExamSessionResponse>> getHistory(User student, Pageable pageable) {
        Page<ExamSessionResponse> page = sessionRepository
                .findByUserOrderByCreatedAtDesc(student, pageable)
                .map(this::toSessionResponseBasic);

        return ApiResponse.ok(ResponseCode.SUCCESS, PageResponse.of(page));
    }

    // ══════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ══════════════════════════════════════════════════════

    private void gradeAndSubmit(ExamSession session) {
        List<ExamAnswer> answers = answerRepository.findBySession(session);

        int correctCount = 0;
        for (ExamAnswer ans : answers) {
            Question q = ans.getQuestion();
            String correctLabel = q.getOptions().stream()
                    .filter(o -> Boolean.TRUE.equals(o.getIsCorrect()))
                    .map(QuestionOption::getLabel)
                    .findFirst()
                    .orElse("");

            boolean isCorrect = correctLabel.equalsIgnoreCase(ans.getAnswerContent());
            ans.setIsCorrect(isCorrect);
            if (isCorrect) correctCount++;
        }
        answerRepository.saveAll(answers);

        BigDecimal score = answers.isEmpty() ? BigDecimal.ZERO :
                BigDecimal.valueOf(correctCount)
                        .multiply(BigDecimal.TEN)
                        .divide(BigDecimal.valueOf(answers.size()), 2, RoundingMode.HALF_UP);

        session.setScore(score);
        session.setEndTime(LocalDateTime.now());
        session.setStatus(SessionStatus.GRADED);
        sessionRepository.save(session);
    }

    private ExamSession findSessionForStudent(UUID sessionId, User student) {
        ExamSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(ResponseCode.EXAM_SESSION_NOT_FOUND));

        if (!session.getUser().getId().equals(student.getId())) {
            throw new ResourceNotFoundException(ResponseCode.EXAM_SESSION_NOT_FOUND);
        }
        return session;
    }

    private void checkSessionActive(ExamSession session) {
        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new BadRequestException(ResponseCode.EXAM_SESSION_ALREADY_SUBMITTED, "Bài thi đã được nộp.");
        }

        // Check timeout
        Exam exam = session.getExam();
        if (exam.getDurationMinutes() != null) {
            LocalDateTime deadline = session.getStartTime().plusMinutes(exam.getDurationMinutes());
            if (LocalDateTime.now().isAfter(deadline)) {
                gradeAndSubmit(session);
                throw new BadRequestException(ResponseCode.EXAM_SESSION_EXPIRED, "Bài thi đã hết thời gian.");
            }
        }
    }

    private ExamSessionResponse toSessionResponse(ExamSession session) {
        List<ExamAnswer> answers = answerRepository.findBySession(session);
        List<ExamQuestion> examQuestions = examQuestionRepository.findByExam(session.getExam());

        List<ExamSessionResponse.QuestionWithAnswer> questions = null;
        if (session.getStatus() == SessionStatus.IN_PROGRESS) {
            questions = examQuestions.stream().map(eq -> {
                Question q = eq.getQuestion();
                String selectedAnswer = answers.stream()
                        .filter(a -> a.getQuestion().getId().equals(q.getId()))
                        .map(ExamAnswer::getAnswerContent)
                        .findFirst()
                        .orElse(null);

                // Don't expose isCorrect during exam
                List<OptionResponse> optionDtos = q.getOptions().stream()
                        .map(o -> OptionResponse.builder()
                                .id(o.getId())
                                .label(o.getLabel())
                                .content(o.getContent())
                                .isCorrect(null) // hide during exam
                                .orderIndex(o.getOrderIndex())
                                .build())
                        .toList();

                return ExamSessionResponse.QuestionWithAnswer.builder()
                        .questionId(q.getId())
                        .content(q.getContent())
                        .options(optionDtos)
                        .selectedAnswer(selectedAnswer)
                        .build();
            }).toList();
        }

        int correctCount = (int) answers.stream().filter(a -> Boolean.TRUE.equals(a.getIsCorrect())).count();

        return ExamSessionResponse.builder()
                .id(session.getId())
                .examId(session.getExam().getId())
                .examTitle(session.getExam().getTitle())
                .subjectName(session.getExam().getSubject().getName())
                .durationMinutes(session.getExam().getDurationMinutes())
                .startedAt(session.getStartTime())
                .submittedAt(session.getEndTime())
                .status(session.getStatus())
                .score(session.getScore())
                .correctCount(correctCount)
                .totalQuestions(examQuestions.size())
                .questions(questions)
                .build();
    }

    private ExamSessionResponse toSessionResponseBasic(ExamSession session) {
        return ExamSessionResponse.builder()
                .id(session.getId())
                .examId(session.getExam().getId())
                .examTitle(session.getExam().getTitle())
                .subjectName(session.getExam().getSubject().getName())
                .durationMinutes(session.getExam().getDurationMinutes())
                .startedAt(session.getStartTime())
                .submittedAt(session.getEndTime())
                .status(session.getStatus())
                .score(session.getScore())
                .totalQuestions(session.getExam().getTotalQuestions())
                .build();
    }

    private String getOptionContent(List<QuestionOption> options, String label) {
        return options.stream()
                .filter(o -> label.equalsIgnoreCase(o.getLabel()))
                .map(QuestionOption::getContent)
                .findFirst()
                .orElse("");
    }
}
