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
import com.eduquiz.feature.examroom.entity.RoomParticipant;
import com.eduquiz.feature.examroom.entity.RoomStatus;
import com.eduquiz.feature.examroom.repository.ExamRoomRepository;
import com.eduquiz.feature.examroom.repository.RoomParticipantRepository;
import com.eduquiz.feature.examsession.dto.*;
import com.eduquiz.feature.examsession.entity.*;
import com.eduquiz.feature.examsession.repository.ExamAnswerRepository;
import com.eduquiz.feature.examsession.repository.ExamSessionRepository;
import com.eduquiz.feature.question.entity.Question;
import com.eduquiz.feature.question.entity.QuestionOption;
import com.eduquiz.feature.question.entity.QuestionType;
import com.eduquiz.kafka.dto.AuditEvent;
import com.eduquiz.kafka.dto.ExamGradedEvent;
import com.eduquiz.kafka.dto.ExamSubmissionEvent;
import com.eduquiz.kafka.producer.AuditEventProducer;
import com.eduquiz.kafka.producer.ExamEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
    private final ExamEventProducer examEventProducer;
    private final AuditEventProducer auditEventProducer;

    // ══════════════════════════════════════════════════════
    // START EXAM
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<ExamSessionResponse> startExam(StartExamRequest request, User currentUser) {
        log.info("[ExamSession.start] userId={}, examId={}, roomId={}", currentUser.getId(), request.getExamId(), request.getRoomId());

        Exam exam = findActiveExam(request.getExamId());
        ExamRoom room = resolveRoom(request.getRoomId(), currentUser, exam);

        // Idempotency: return existing in-progress session
        Optional<ExamSession> existing = findExistingSession(currentUser, exam, room);
        if (existing.isPresent()) {
            ExamSession session = existing.get();
            if (session.isFinished()) {
                throw new BadRequestException(ResponseCode.EXAM_SESSION_ALREADY_SUBMITTED);
            }
            if (session.isExpired()) {
                // Auto-submit silently and return error
                doSubmit(session, SubmissionSource.AUTO_TIMEOUT);
                throw new BadRequestException(ResponseCode.EXAM_SESSION_EXPIRED);
            }
            log.info("[ExamSession.start] Resuming existing session={}", session.getId());
            return ApiResponse.ok(ResponseCode.EXAM_SESSION_STARTED, toSessionResponse(session));
        }

        // Create new session
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(exam.getDurationMinutes());

        ExamSession session = ExamSession.builder()
                .user(currentUser)
                .exam(exam)
                .room(room)
                .startTime(now)
                .endTime(expiresAt)
                .status(SessionStatus.IN_PROGRESS)
                .build();
        session = sessionRepository.saveAndFlush(session);

        log.info("[ExamSession.start] Created session={}, expiresAt={}", session.getId(), expiresAt);

        // Publish audit event
        final UUID sessionId = session.getId();
        auditEventProducer.log(currentUser.getId(), "START_EXAM", "ExamSession", sessionId,
                Map.of("examId", exam.getId(), "roomId", room != null ? room.getId() : "practice"));

        return ApiResponse.ok(ResponseCode.EXAM_SESSION_STARTED, toSessionResponse(session));
    }

    // ══════════════════════════════════════════════════════
    // GET CURRENT SESSION (for resume / reload)
    // ══════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public ApiResponse<ExamSessionResponse> getCurrentSession(UUID examId, UUID roomId, User currentUser) {
        Exam exam = findActiveExam(examId);
        ExamRoom room = roomId != null
                ? roomRepository.findById(roomId).orElseThrow(() -> new ResourceNotFoundException(ResponseCode.ROOM_NOT_FOUND))
                : null;

        ExamSession session = findExistingSession(currentUser, exam, room)
                .orElseThrow(() -> new ResourceNotFoundException(ResponseCode.EXAM_SESSION_NOT_FOUND));

        return ApiResponse.ok(ResponseCode.EXAM_SESSION_FETCH_SUCCESS, toSessionResponse(session));
    }

    // ══════════════════════════════════════════════════════
    // GET SESSION BY ID
    // ══════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public ApiResponse<ExamSessionResponse> getSession(UUID sessionId, User currentUser) {
        ExamSession session = findSession(sessionId);
        checkSessionReadAccess(session, currentUser);
        return ApiResponse.ok(ResponseCode.EXAM_SESSION_FETCH_SUCCESS, toSessionResponse(session));
    }

    // ══════════════════════════════════════════════════════
    // SAVE SINGLE ANSWER
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<Void> saveAnswer(UUID sessionId, AnswerRequest request, User currentUser) {
        ExamSession session = findSession(sessionId);
        validateSessionForAnswer(session, currentUser);

        Question question = resolveQuestionInSession(request.getQuestionId(), session);
        validateOptions(request.getSelectedOptions(), question);

        upsertAnswer(session, question, request.getSelectedOptions());

        log.debug("[ExamSession.saveAnswer] session={}, question={}", sessionId, request.getQuestionId());
        return ApiResponse.ok(ResponseCode.EXAM_ANSWER_SAVED);
    }

    // ══════════════════════════════════════════════════════
    // BATCH SAVE ANSWERS (autosave)
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<Void> batchSaveAnswers(UUID sessionId, BatchAnswerRequest request, User currentUser) {
        ExamSession session = findSession(sessionId);
        validateSessionForAnswer(session, currentUser);

        List<ExamQuestion> examQuestions = examQuestionRepository.findByExam(session.getExam());
        Set<UUID> validQuestionIds = examQuestions.stream()
                .map(eq -> eq.getQuestion().getId())
                .collect(Collectors.toSet());

        for (AnswerRequest ans : request.getAnswers()) {
            if (!validQuestionIds.contains(ans.getQuestionId())) {
                throw new BadRequestException(ResponseCode.EXAM_QUESTION_NOT_IN_SESSION,
                        "Question not in session: " + ans.getQuestionId());
            }
            Question question = examQuestions.stream()
                    .filter(eq -> eq.getQuestion().getId().equals(ans.getQuestionId()))
                    .findFirst().get().getQuestion();
            validateOptions(ans.getSelectedOptions(), question);
            upsertAnswer(session, question, ans.getSelectedOptions());
        }

        log.debug("[ExamSession.batchSave] session={}, count={}", sessionId, request.getAnswers().size());
        return ApiResponse.ok(ResponseCode.EXAM_ANSWERS_SAVED);
    }

    // ══════════════════════════════════════════════════════
    // SUBMIT EXAM (manual)
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<ExamResultResponse> submitExam(UUID sessionId, User currentUser) {
        // Pessimistic lock to prevent double-submit race condition
        ExamSession session = sessionRepository.findByIdWithLock(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(ResponseCode.EXAM_SESSION_NOT_FOUND));

        if (!session.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException(ResponseCode.AUTH_FORBIDDEN);
        }
        if (session.isFinished()) {
            throw new BadRequestException(ResponseCode.EXAM_SESSION_ALREADY_SUBMITTED);
        }

        ExamSession submitted = doSubmit(session, SubmissionSource.MANUAL);
        log.info("[ExamSession.submit] session={}, score={}", sessionId, submitted.getScore());

        // Publish Kafka events (after transaction commits)
        publishKafkaEvents(submitted);

        return ApiResponse.ok(ResponseCode.EXAM_SUBMIT_SUCCESS, toResultResponse(submitted));
    }

    // ══════════════════════════════════════════════════════
    // GET RESULT
    // ══════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public ApiResponse<ExamResultResponse> getResult(UUID sessionId, User currentUser) {
        ExamSession session = findSession(sessionId);
        checkSessionReadAccess(session, currentUser);

        if (!session.isFinished()) {
            throw new BadRequestException(ResponseCode.EXAM_RESULT_NOT_READY);
        }

        return ApiResponse.ok(ResponseCode.EXAM_RESULT_FETCH_SUCCESS, toResultResponse(session));
    }

    // ══════════════════════════════════════════════════════
    // GET HISTORY (student's own sessions)
    // ══════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public ApiResponse<PageResponse<ExamResultResponse>> getHistory(User currentUser, Pageable pageable) {
        Page<ExamResultResponse> page = sessionRepository
                .findByUserOrderByCreatedAtDesc(currentUser, pageable)
                .map(s -> toResultResponse(s));
        return ApiResponse.ok(ResponseCode.EXAM_SESSION_LIST_SUCCESS, PageResponse.of(page));
    }

    // ══════════════════════════════════════════════════════
    // GET EXAM RESULTS (teacher view)
    // ══════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public ApiResponse<PageResponse<ExamResultResponse>> getExamResults(UUID examId, User currentUser, Pageable pageable) {
        Exam exam = findActiveExam(examId);
        checkTeacherExamAccess(exam, currentUser);

        Page<ExamResultResponse> page = sessionRepository
                .findByExamOrderByCreatedAtDesc(exam, pageable)
                .map(s -> toResultResponse(s));
        return ApiResponse.ok(ResponseCode.EXAM_SESSION_LIST_SUCCESS, PageResponse.of(page));
    }

    // ══════════════════════════════════════════════════════
    // AUTO-SUBMIT SCHEDULER (runs every 60 seconds)
    // ══════════════════════════════════════════════════════

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void autoSubmitExpiredSessions() {
        List<ExamSession> timedOut = sessionRepository.findTimedOutSessions(LocalDateTime.now());
        if (timedOut.isEmpty()) return;

        log.info("[ExamSession.scheduler] Auto-submitting {} expired sessions", timedOut.size());
        for (ExamSession session : timedOut) {
            try {
                ExamSession submitted = doSubmit(session, SubmissionSource.AUTO_TIMEOUT);
                publishKafkaEvents(submitted);
            } catch (Exception e) {
                log.error("[ExamSession.scheduler] Failed to auto-submit session={}: {}", session.getId(), e.getMessage());
            }
        }
    }

    // ══════════════════════════════════════════════════════
    // PRIVATE: CORE SUBMIT + GRADE
    // ══════════════════════════════════════════════════════

    private ExamSession doSubmit(ExamSession session, SubmissionSource source) {
        if (session.isFinished()) {
            return session; // idempotent - already submitted
        }

        List<ExamQuestion> examQuestions = examQuestionRepository.findByExam(session.getExam());
        List<ExamAnswer> answers = answerRepository.findBySession(session);

        // Index existing answers by questionId
        Map<UUID, ExamAnswer> answerMap = answers.stream()
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), a -> a));

        int correctCount = 0;

        for (ExamQuestion eq : examQuestions) {
            Question question = eq.getQuestion();
            ExamAnswer answer = answerMap.get(question.getId());

            if (answer == null) {
                // Unanswered — create a blank graded answer
                ExamAnswer blank = ExamAnswer.builder()
                        .session(session)
                        .question(question)
                        .answerContent(null)
                        .isCorrect(false)
                        .build();
                answerRepository.save(blank);
                continue;
            }

            boolean correct = gradeAnswer(question, answer.getAnswerContent());
            answer.setIsCorrect(correct);
            answerRepository.save(answer);
            if (correct) correctCount++;
        }

        int total = examQuestions.size();
        BigDecimal score = total > 0
                ? BigDecimal.valueOf(10.0 * correctCount / total).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        SessionStatus finalStatus = source == SubmissionSource.AUTO_TIMEOUT
                ? SessionStatus.AUTO_SUBMITTED
                : SessionStatus.SUBMITTED;

        session.setStatus(finalStatus);
        session.setSubmissionSource(source);
        session.setSubmittedAt(LocalDateTime.now());
        session.setScore(score);
        session.setCorrectCount(correctCount);
        sessionRepository.save(session);

        return session;
    }

    /**
     * Grade a single answer against the question's correct options.
     * SINGLE_CHOICE: exact label match with the one correct option.
     * MULTI_CHOICE: selected set == correct set (all or nothing).
     */
    private boolean gradeAnswer(Question question, String answerContent) {
        if (answerContent == null || answerContent.isBlank()) return false;

        Set<String> correctLabels = question.getOptions().stream()
                .filter(QuestionOption::getIsCorrect)
                .map(QuestionOption::getLabel)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        Set<String> selected = Arrays.stream(answerContent.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        if (question.getType() == QuestionType.SINGLE_CHOICE) {
            return selected.size() == 1 && correctLabels.containsAll(selected) && selected.containsAll(correctLabels);
        } else if (question.getType() == QuestionType.MULTI_CHOICE) {
            return selected.equals(correctLabels);
        }
        return false;
    }

    // ══════════════════════════════════════════════════════
    // PRIVATE: UPSERT ANSWER
    // ══════════════════════════════════════════════════════

    private void upsertAnswer(ExamSession session, Question question, List<String> selectedOptions) {
        String content = formatAnswerContent(selectedOptions);

        ExamAnswer answer = answerRepository.findBySessionAndQuestion(session, question)
                .orElseGet(() -> ExamAnswer.builder()
                        .session(session)
                        .question(question)
                        .build());

        answer.setAnswerContent(content);
        answer.setIsCorrect(null); // reset — will be graded on submit
        answerRepository.save(answer);
    }

    /**
     * Normalize to sorted comma-separated labels. ["C", "A"] → "A,C"
     * Empty/null list → null (clear answer).
     */
    private String formatAnswerContent(List<String> selectedOptions) {
        if (selectedOptions == null || selectedOptions.isEmpty()) return null;
        return selectedOptions.stream()
                .map(String::trim)
                .map(String::toUpperCase)
                .distinct()
                .sorted()
                .collect(Collectors.joining(","));
    }

    // ══════════════════════════════════════════════════════
    // PRIVATE: VALIDATION HELPERS
    // ══════════════════════════════════════════════════════

    private void validateSessionForAnswer(ExamSession session, User currentUser) {
        if (!session.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException(ResponseCode.AUTH_FORBIDDEN);
        }
        if (session.isFinished()) {
            throw new BadRequestException(ResponseCode.EXAM_SESSION_ALREADY_SUBMITTED);
        }
        if (session.isExpired()) {
            // Trigger auto-submit for expired session
            doSubmit(session, SubmissionSource.AUTO_TIMEOUT);
            throw new BadRequestException(ResponseCode.EXAM_SESSION_EXPIRED);
        }
    }

    private Question resolveQuestionInSession(UUID questionId, ExamSession session) {
        List<ExamQuestion> examQuestions = examQuestionRepository.findByExam(session.getExam());
        return examQuestions.stream()
                .filter(eq -> eq.getQuestion().getId().equals(questionId))
                .findFirst()
                .map(ExamQuestion::getQuestion)
                .orElseThrow(() -> new BadRequestException(ResponseCode.EXAM_QUESTION_NOT_IN_SESSION));
    }

    private void validateOptions(List<String> selectedOptions, Question question) {
        if (selectedOptions == null || selectedOptions.isEmpty()) return; // clearing is OK

        Set<String> validLabels = question.getOptions().stream()
                .map(QuestionOption::getLabel)
                .collect(Collectors.toSet());

        for (String opt : selectedOptions) {
            if (!validLabels.contains(opt.trim().toUpperCase())) {
                throw new BadRequestException(ResponseCode.EXAM_ANSWER_INVALID,
                        "Invalid option label: " + opt);
            }
        }

        if (question.getType() == QuestionType.SINGLE_CHOICE && selectedOptions.size() > 1) {
            throw new BadRequestException(ResponseCode.EXAM_ANSWER_INVALID,
                    "SINGLE_CHOICE accepts only one option");
        }
    }

    private ExamRoom resolveRoom(UUID roomId, User currentUser, Exam exam) {
        if (roomId == null) return null; // practice mode

        ExamRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException(ResponseCode.ROOM_NOT_FOUND));

        if (room.getStatus() != RoomStatus.OPEN && room.getStatus() != RoomStatus.IN_PROGRESS) {
            throw new BadRequestException(ResponseCode.ROOM_NOT_OPEN);
        }

        // Verify student is a registered participant
        RoomParticipant.RoomParticipantId participantId =
                new RoomParticipant.RoomParticipantId(roomId, currentUser.getId());
        if (!participantRepository.existsById(participantId)) {
            throw new BadRequestException(ResponseCode.AUTH_FORBIDDEN,
                    "You are not registered in this room");
        }

        return room;
    }

    private Optional<ExamSession> findExistingSession(User user, Exam exam, ExamRoom room) {
        if (room == null) {
            return sessionRepository.findByUserAndExamAndRoomIsNullAndStatus(
                    user, exam, SessionStatus.IN_PROGRESS);
        }
        return sessionRepository.findByUserAndExamAndRoomAndStatus(
                user, exam, room, SessionStatus.IN_PROGRESS);
    }

    private void checkSessionReadAccess(ExamSession session, User currentUser) {
        String role = currentUser.getRole().getName();
        if ("ADMIN".equals(role)) return;
        if (session.getUser().getId().equals(currentUser.getId())) return;
        if ("TEACHER".equals(role) && session.getExam().getCreator().getId().equals(currentUser.getId())) return;
        throw new BadRequestException(ResponseCode.AUTH_FORBIDDEN);
    }

    private void checkTeacherExamAccess(Exam exam, User currentUser) {
        String role = currentUser.getRole().getName();
        if ("ADMIN".equals(role)) return;
        if ("TEACHER".equals(role) && exam.getCreator().getId().equals(currentUser.getId())) return;
        throw new BadRequestException(ResponseCode.AUTH_FORBIDDEN);
    }

    // ══════════════════════════════════════════════════════
    // PRIVATE: KAFKA EVENT PUBLISHING
    // ══════════════════════════════════════════════════════

    private void publishKafkaEvents(ExamSession session) {
        try {
            UUID subjectId = session.getExam().getSubject().getId();
            UUID roomId    = session.getRoom() != null ? session.getRoom().getId() : null;
            int total      = examQuestionRepository.countByExam(session.getExam());

            // 1. exam-submission (for GradingConsumer audit trail)
            examEventProducer.publishSubmission(ExamSubmissionEvent.builder()
                    .sessionId(session.getId())
                    .userId(session.getUser().getId())
                    .examId(session.getExam().getId())
                    .subjectId(subjectId)
                    .roomId(roomId)
                    .submittedAt(session.getSubmittedAt())
                    .build());

            // 2. exam-graded (for LeaderboardConsumer)
            examEventProducer.publishGraded(ExamGradedEvent.builder()
                    .sessionId(session.getId())
                    .userId(session.getUser().getId())
                    .examId(session.getExam().getId())
                    .subjectId(subjectId)
                    .roomId(roomId)
                    .score(session.getScore())
                    .correctCount(session.getCorrectCount())
                    .totalQuestions(total)
                    .gradedAt(session.getSubmittedAt())
                    .build());

            // 3. audit-event
            auditEventProducer.log(session.getUser().getId(), "SUBMIT_EXAM",
                    "ExamSession", session.getId(),
                    Map.of(
                            "examId",  session.getExam().getId(),
                            "score",   session.getScore(),
                            "source",  session.getSubmissionSource()
                    ));
        } catch (Exception e) {
            log.error("[ExamSession.kafka] Failed to publish events for session={}: {}",
                    session.getId(), e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════
    // PRIVATE: FINDERS
    // ══════════════════════════════════════════════════════

    private Exam findActiveExam(UUID examId) {
        return examRepository.findByIdAndIsActiveTrue(examId)
                .orElseThrow(() -> new ResourceNotFoundException(ResponseCode.EXAM_NOT_FOUND));
    }

    private ExamSession findSession(UUID sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(ResponseCode.EXAM_SESSION_NOT_FOUND));
    }

    // ══════════════════════════════════════════════════════
    // PRIVATE: RESPONSE MAPPERS
    // ══════════════════════════════════════════════════════

    private ExamSessionResponse toSessionResponse(ExamSession session) {
        List<ExamQuestion> examQuestions = examQuestionRepository.findByExam(session.getExam());
        List<ExamAnswer> answers = answerRepository.findBySession(session);
        Map<UUID, String> answerMap = answers.stream()
                .filter(a -> a.getAnswerContent() != null)
                .collect(Collectors.toMap(
                        a -> a.getQuestion().getId(),
                        ExamAnswer::getAnswerContent
                ));

        List<ExamSessionResponse.QuestionWithAnswer> questions = examQuestions.stream()
                .map(eq -> {
                    Question q = eq.getQuestion();
                    String selected = answerMap.get(q.getId());

                    List<ExamSessionResponse.OptionInfo> options = q.getOptions().stream()
                            .map(o -> ExamSessionResponse.OptionInfo.builder()
                                    .optionId(o.getId())
                                    .label(o.getLabel())
                                    .content(o.getContent())
                                    // isCorrect intentionally omitted
                                    .build())
                            .toList();

                    return ExamSessionResponse.QuestionWithAnswer.builder()
                            .questionId(q.getId())
                            .content(q.getContent())
                            .type(q.getType())
                            .options(options)
                            .selectedAnswer(selected)
                            .build();
                })
                .toList();

        return ExamSessionResponse.builder()
                .id(session.getId())
                .examId(session.getExam().getId())
                .examTitle(session.getExam().getTitle())
                .subjectName(session.getExam().getSubject().getName())
                .durationMinutes(session.getExam().getDurationMinutes())
                .startedAt(session.getStartTime())
                .expiresAt(session.getEndTime())
                .remainingSeconds(session.getRemainingSeconds())
                .status(session.getStatus())
                .totalQuestions(examQuestions.size())
                .questions(questions)
                .build();
    }

    private ExamResultResponse toResultResponse(ExamSession session) {
        List<ExamAnswer> answers = answerRepository.findBySession(session);
        Map<UUID, ExamAnswer> answerMap = answers.stream()
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), a -> a));

        List<ExamQuestion> examQuestions = examQuestionRepository.findByExam(session.getExam());

        int unanswered = 0;
        int incorrect = 0;

        List<ExamResultResponse.AnswerDetail> details = new ArrayList<>();
        for (ExamQuestion eq : examQuestions) {
            Question q = eq.getQuestion();
            ExamAnswer answer = answerMap.get(q.getId());

            List<String> selected = null;
            Boolean isCorrect = null;
            if (answer != null && answer.getAnswerContent() != null) {
                selected = Arrays.asList(answer.getAnswerContent().split(","));
                isCorrect = answer.getIsCorrect();
            } else {
                unanswered++;
                isCorrect = false;
            }
            if (Boolean.FALSE.equals(isCorrect) && (answer == null || answer.getAnswerContent() != null)) {
                incorrect++;
            }

            List<String> correctOptions = q.getOptions().stream()
                    .filter(QuestionOption::getIsCorrect)
                    .map(QuestionOption::getLabel)
                    .sorted()
                    .toList();

            List<ExamResultResponse.OptionDetail> optionDetails = q.getOptions().stream()
                    .map(o -> ExamResultResponse.OptionDetail.builder()
                            .label(o.getLabel())
                            .content(o.getContent())
                            .isCorrect(o.getIsCorrect())
                            .build())
                    .toList();

            details.add(ExamResultResponse.AnswerDetail.builder()
                    .questionId(q.getId())
                    .questionContent(q.getContent())
                    .questionType(q.getType())
                    .options(optionDetails)
                    .selectedOptions(selected)
                    .correctOptions(correctOptions)
                    .isCorrect(isCorrect)
                    .explanation(q.getExplanation())
                    .build());
        }

        ExamRoom room = session.getRoom();

        return ExamResultResponse.builder()
                .sessionId(session.getId())
                .examId(session.getExam().getId())
                .examTitle(session.getExam().getTitle())
                .subjectName(session.getExam().getSubject().getName())
                .examCreatorName(session.getExam().getCreator() != null
                        ? session.getExam().getCreator().getFullName() : null)
                .studentName(session.getUser().getFullName())
                .roomTitle(room != null ? room.getTitle() : null)
                .roomCode(room != null ? room.getRoomCode() : null)
                .teacherName(room != null && room.getCreatedBy() != null
                        ? room.getCreatedBy().getFullName() : null)
                .status(session.getStatus())
                .submissionSource(session.getSubmissionSource())
                .score(session.getScore())
                .correctCount(session.getCorrectCount())
                .incorrectCount(incorrect)
                .unansweredCount(unanswered)
                .totalQuestions(examQuestions.size())
                .startedAt(session.getStartTime())
                .submittedAt(session.getSubmittedAt())
                .answers(details)
                .build();
    }
}
