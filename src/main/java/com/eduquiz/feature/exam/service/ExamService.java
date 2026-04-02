package com.eduquiz.feature.exam.service;

import com.eduquiz.common.constant.ResponseCode;
import com.eduquiz.common.dto.ApiResponse;
import com.eduquiz.common.dto.PageResponse;
import com.eduquiz.common.exception.BadRequestException;
import com.eduquiz.common.exception.ResourceNotFoundException;
import com.eduquiz.feature.auth.entity.User;
import com.eduquiz.feature.exam.dto.CreateExamRequest;
import com.eduquiz.feature.exam.dto.ExamResponse;
import com.eduquiz.feature.exam.entity.*;
import com.eduquiz.feature.exam.repository.ExamQuestionRepository;
import com.eduquiz.feature.exam.repository.ExamRepository;
import com.eduquiz.feature.question.dto.OptionResponse;
import com.eduquiz.feature.question.dto.QuestionResponse;
import com.eduquiz.feature.question.entity.Question;
import com.eduquiz.feature.question.repository.QuestionRepository;
import com.eduquiz.feature.subject.entity.Subject;
import com.eduquiz.feature.subject.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExamService {

    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final QuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;

    // ══════════════════════════════════════════════════════
    // GET EXAMS BY SUBJECT (paginated, filter by examType/year)
    // ══════════════════════════════════════════════════════

    public ApiResponse<PageResponse<ExamResponse>> getExamsBySubject(
            UUID subjectId, ExamType examType, Integer year, Pageable pageable) {
        log.info("[ExamService.getExamsBySubject] START - subjectId={}, examType={}, year={}", subjectId, examType, year);

        findActiveSubject(subjectId);

        Page<Exam> page;
        if (year != null) {
            page = examRepository.findBySubjectIdAndYearAndIsActiveTrueOrderByCreatedAtDesc(subjectId, year, pageable);
        } else if (examType != null) {
            page = examRepository.findBySubjectIdAndExamTypeAndIsActiveTrueOrderByYearDescCreatedAtDesc(subjectId, examType, pageable);
        } else {
            page = examRepository.findBySubjectIdAndIsActiveTrueOrderByYearDescCreatedAtDesc(subjectId, pageable);
        }

        Page<ExamResponse> responsePage = page.map(this::toResponse);

        log.info("[ExamService.getExamsBySubject] SUCCESS - subjectId={}, totalElements={}", subjectId, page.getTotalElements());
        return ApiResponse.ok(ResponseCode.EXAM_LIST_SUCCESS, PageResponse.of(responsePage));
    }

    // ══════════════════════════════════════════════════════
    // GET ALL EXAMS (paginated)
    // ══════════════════════════════════════════════════════

    public ApiResponse<PageResponse<ExamResponse>> getAllExams(Pageable pageable) {
        log.info("[ExamService.getAllExams] START - page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());

        Page<ExamResponse> page = examRepository.findByIsActiveTrueOrderByCreatedAtDesc(pageable)
                .map(this::toResponse);

        log.info("[ExamService.getAllExams] SUCCESS - totalElements={}", page.getTotalElements());
        return ApiResponse.ok(ResponseCode.EXAM_LIST_SUCCESS, PageResponse.of(page));
    }

    // ══════════════════════════════════════════════════════
    // GET EXAM DETAIL (with questions)
    // ══════════════════════════════════════════════════════

    public ApiResponse<ExamResponse> getExamById(UUID examId) {
        log.info("[ExamService.getExamById] START - examId={}", examId);

        Exam exam = findActiveExam(examId);
        ExamResponse response = toDetailResponse(exam);

        log.info("[ExamService.getExamById] SUCCESS - examId={}, title={}", examId, exam.getTitle());
        return ApiResponse.ok(ResponseCode.EXAM_FETCH_SUCCESS, response);
    }

    // ══════════════════════════════════════════════════════
    // CREATE EXAM
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<ExamResponse> createExam(CreateExamRequest request) {
        log.info("[ExamService.createExam] START - title={}, randomMode={}, examType={}", request.getTitle(), request.getRandomMode(), request.getExamType());

        Subject subject = findActiveSubject(request.getSubjectId());
        User currentUser = getCurrentUser();

        Integer duration = request.getDurationMinutes() != null
                ? request.getDurationMinutes()
                : subject.getDefaultDurationMinutes();

        Exam exam = Exam.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .creator(currentUser)
                .subject(subject)
                .durationMinutes(duration)
                .randomMode(request.getRandomMode())
                .year(request.getYear())
                .examType(request.getExamType() != null ? request.getExamType() : ExamType.PRACTICE)
                .build();

        List<Question> questions;

        switch (request.getRandomMode()) {
            case MANUAL -> {
                if (request.getQuestionIds() == null || request.getQuestionIds().isEmpty()) {
                    throw new BadRequestException(ResponseCode.EXAM_NO_QUESTIONS,
                            "Chế độ MANUAL phải chọn ít nhất 1 câu hỏi");
                }
                questions = request.getQuestionIds().stream()
                        .map(qId -> questionRepository.findByIdAndIsActiveTrue(qId)
                                .orElseThrow(() -> new ResourceNotFoundException(ResponseCode.QUESTION_NOT_FOUND,
                                        "Câu hỏi không tồn tại: " + qId)))
                        .toList();
            }
            case FULL_RANDOM -> {
                int total = request.getTotalQuestions() != null ? request.getTotalQuestions() : 40;
                questions = questionRepository.findRandomBySubjectId(
                        request.getSubjectId(), PageRequest.of(0, total));
                if (questions.isEmpty()) {
                    throw new BadRequestException(ResponseCode.EXAM_NO_QUESTIONS,
                            "Không có câu hỏi nào trong môn học này");
                }
            }
            case POOL_RANDOM -> {
                if (request.getChapterIds() == null || request.getChapterIds().isEmpty()) {
                    throw new BadRequestException(ResponseCode.EXAM_NO_QUESTIONS,
                            "Chế độ POOL_RANDOM phải chọn ít nhất 1 chương");
                }
                int total = request.getTotalQuestions() != null ? request.getTotalQuestions() : 40;
                if (request.getDifficulty() != null) {
                    questions = questionRepository.findRandomByChapterIdsAndDifficulty(
                            request.getChapterIds(), request.getDifficulty(), PageRequest.of(0, total));
                } else {
                    questions = questionRepository.findRandomBySubjectId(
                            request.getSubjectId(), PageRequest.of(0, total));
                }
                if (questions.isEmpty()) {
                    throw new BadRequestException(ResponseCode.EXAM_NO_QUESTIONS,
                            "Không có câu hỏi phù hợp để tạo đề");
                }
            }
            default -> throw new BadRequestException(ResponseCode.BAD_REQUEST, "Chế độ tạo đề không hợp lệ");
        }

        exam.setTotalQuestions(questions.size());
        examRepository.save(exam);

        // Save exam_questions
        List<ExamQuestion> examQuestions = questions.stream()
                .map(q -> ExamQuestion.builder()
                        .id(new ExamQuestion.ExamQuestionId(exam.getId(), q.getId()))
                        .exam(exam)
                        .question(q)
                        .build())
                .toList();
        examQuestionRepository.saveAll(examQuestions);

        log.info("[ExamService.createExam] SUCCESS - id={}, title={}, totalQuestions={}", exam.getId(), exam.getTitle(), questions.size());
        return ApiResponse.ok(ResponseCode.EXAM_CREATED_SUCCESS, toResponse(exam));
    }

    // ══════════════════════════════════════════════════════
    // UPDATE EXAM
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<ExamResponse> updateExam(UUID examId, CreateExamRequest request) {
        log.info("[ExamService.updateExam] START - examId={}", examId);

        Exam exam = findActiveExam(examId);

        exam.setTitle(request.getTitle());
        exam.setDescription(request.getDescription());
        if (request.getDurationMinutes() != null) {
            exam.setDurationMinutes(request.getDurationMinutes());
        }
        if (request.getYear() != null) {
            exam.setYear(request.getYear());
        }
        if (request.getExamType() != null) {
            exam.setExamType(request.getExamType());
        }
        examRepository.save(exam);

        log.info("[ExamService.updateExam] SUCCESS - id={}, title={}", examId, exam.getTitle());
        return ApiResponse.ok(ResponseCode.EXAM_UPDATED_SUCCESS, toResponse(exam));
    }

    // ══════════════════════════════════════════════════════
    // DELETE EXAM (soft delete)
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<Void> deleteExam(UUID examId) {
        log.info("[ExamService.deleteExam] START - examId={}", examId);

        Exam exam = findActiveExam(examId);
        exam.setIsActive(false);
        examRepository.save(exam);

        log.info("[ExamService.deleteExam] SUCCESS - id={}, softDeleted=true", examId);
        return ApiResponse.ok(ResponseCode.EXAM_DELETED_SUCCESS);
    }

    // ══════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ══════════════════════════════════════════════════════

    private Subject findActiveSubject(UUID subjectId) {
        return subjectRepository.findByIdAndIsActiveTrue(subjectId)
                .orElseThrow(() -> {
                    log.warn("[ExamService] FAILED - subject not found: {}", subjectId);
                    return new ResourceNotFoundException(ResponseCode.SUBJECT_NOT_FOUND);
                });
    }

    private Exam findActiveExam(UUID examId) {
        return examRepository.findByIdAndIsActiveTrue(examId)
                .orElseThrow(() -> {
                    log.warn("[ExamService] FAILED - exam not found: {}", examId);
                    return new ResourceNotFoundException(ResponseCode.EXAM_NOT_FOUND);
                });
    }

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // ══════════════════════════════════════════════════════
    // TOGGLE SHARE
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<ExamResponse> toggleShare(UUID examId) {
        log.info("[ExamService.toggleShare] START - examId={}", examId);
        Exam exam = findActiveExam(examId);

        User currentUser = getCurrentUser();
        if (!exam.getCreator().getId().equals(currentUser.getId())) {
            throw new BadRequestException(ResponseCode.EXAM_NOT_FOUND, "Bạn không phải chủ sở hữu đề thi.");
        }

        boolean newValue = !Boolean.TRUE.equals(exam.getIsShared());
        exam.setIsShared(newValue);
        examRepository.save(exam);

        log.info("[ExamService.toggleShare] SUCCESS - examId={}, isShared={}", examId, newValue);
        ResponseCode code = newValue ? ResponseCode.EXAM_SHARED_SUCCESS : ResponseCode.EXAM_UNSHARED_SUCCESS;
        return ApiResponse.ok(code, toResponse(exam));
    }

    private ExamResponse toResponse(Exam exam) {
        return ExamResponse.builder()
                .id(exam.getId())
                .title(exam.getTitle())
                .description(exam.getDescription())
                .subjectId(exam.getSubject().getId())
                .subjectName(exam.getSubject().getName())
                .durationMinutes(exam.getDurationMinutes())
                .totalQuestions(exam.getTotalQuestions())
                .randomMode(exam.getRandomMode())
                .year(exam.getYear())
                .examType(exam.getExamType())
                .createdById(exam.getCreator().getId())
                .createdByName(exam.getCreator().getFullName())
                .isShared(exam.getIsShared())
                .createdAt(exam.getCreatedAt())
                .updatedAt(exam.getUpdatedAt())
                .build();
    }

    private ExamResponse toDetailResponse(Exam exam) {
        List<ExamQuestion> examQuestions = examQuestionRepository.findByExam(exam);
        List<QuestionResponse> questionResponses = examQuestions.stream()
                .map(eq -> {
                    Question q = eq.getQuestion();
                    List<OptionResponse> options = q.getOptions().stream()
                            .map(o -> OptionResponse.builder()
                                    .id(o.getId())
                                    .label(o.getLabel())
                                    .content(o.getContent())
                                    .isCorrect(o.getIsCorrect())
                                    .orderIndex(o.getOrderIndex())
                                    .build())
                            .toList();
                    return QuestionResponse.builder()
                            .id(q.getId())
                            .chapterId(q.getChapter().getId())
                            .chapterName(q.getChapter().getName())
                            .subjectId(q.getChapter().getSubject().getId())
                            .subjectName(q.getChapter().getSubject().getName())
                            .content(q.getContent())
                            .type(q.getType())
                            .difficulty(q.getDifficulty())
                            .explanation(q.getExplanation())
                            .options(options)
                            .createdAt(q.getCreatedAt())
                            .updatedAt(q.getUpdatedAt())
                            .build();
                })
                .toList();

        ExamResponse response = toResponse(exam);
        response.setQuestions(questionResponses);
        return response;
    }
}
