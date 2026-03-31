package com.eduquiz.feature.question.service;

import com.eduquiz.common.constant.ResponseCode;
import com.eduquiz.common.dto.ApiResponse;
import com.eduquiz.common.dto.PageResponse;
import com.eduquiz.common.exception.BadRequestException;
import com.eduquiz.common.exception.ResourceNotFoundException;
import com.eduquiz.common.specification.QuestionSpecification;
import com.eduquiz.feature.auth.entity.User;
import com.eduquiz.feature.chapter.entity.Chapter;
import com.eduquiz.feature.chapter.repository.ChapterRepository;
import com.eduquiz.feature.question.dto.*;
import com.eduquiz.feature.question.entity.*;
import com.eduquiz.feature.question.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final ChapterRepository chapterRepository;

    // ══════════════════════════════════════════════════════
    // GET QUESTIONS BY CHAPTER (public, paginated + filter)
    // ══════════════════════════════════════════════════════

    public ApiResponse<PageResponse<QuestionResponse>> getQuestionsByChapter(
            UUID chapterId, Difficulty difficulty, QuestionType type, Pageable pageable) {
        log.info("[QuestionService.getQuestionsByChapter] START - chapterId={}, difficulty={}, type={}", chapterId, difficulty, type);

        findActiveChapter(chapterId);

        Specification<Question> spec = Specification.where(QuestionSpecification.isActive())
                .and(QuestionSpecification.hasChapterId(chapterId))
                .and(QuestionSpecification.hasDifficulty(difficulty))
                .and(QuestionSpecification.hasType(type));

        Page<Question> page = questionRepository.findAll(spec, pageable);
        Page<QuestionResponse> responsePage = page.map(q -> toResponse(q, null));

        log.info("[QuestionService.getQuestionsByChapter] SUCCESS - chapterId={}, totalElements={}", chapterId, page.getTotalElements());
        return ApiResponse.ok(ResponseCode.QUESTION_LIST_SUCCESS, PageResponse.of(responsePage));
    }

    // ══════════════════════════════════════════════════════
    // GET MY QUESTIONS (role-based: ADMIN sees all, TEACHER sees own + shared)
    // ══════════════════════════════════════════════════════

    public ApiResponse<PageResponse<QuestionResponse>> getMyQuestions(
            User currentUser, UUID chapterId, UUID subjectId,
            Difficulty difficulty, QuestionType type, String keyword, Pageable pageable) {
        log.info("[QuestionService.getMyQuestions] START - userId={}, role={}", currentUser.getId(), currentUser.getRole().getName());

        Specification<Question> spec = Specification.where(QuestionSpecification.isActive());

        // ADMIN thấy tất cả, TEACHER chỉ thấy câu mình tạo + câu được share
        if (!isAdmin(currentUser)) {
            spec = spec.and(QuestionSpecification.visibleToTeacher(currentUser.getId()));
        }

        spec = spec
                .and(QuestionSpecification.hasChapterId(chapterId))
                .and(QuestionSpecification.hasSubjectId(subjectId))
                .and(QuestionSpecification.hasDifficulty(difficulty))
                .and(QuestionSpecification.hasType(type))
                .and(QuestionSpecification.hasKeyword(keyword));

        Page<Question> page = questionRepository.findAll(spec, pageable);
        Page<QuestionResponse> responsePage = page.map(q -> toResponse(q, currentUser));

        log.info("[QuestionService.getMyQuestions] SUCCESS - userId={}, totalElements={}", currentUser.getId(), page.getTotalElements());
        return ApiResponse.ok(ResponseCode.QUESTION_LIST_SUCCESS, PageResponse.of(responsePage));
    }

    // ══════════════════════════════════════════════════════
    // GET QUESTION BY ID (public)
    // ══════════════════════════════════════════════════════

    public ApiResponse<QuestionResponse> getQuestionById(UUID questionId) {
        log.info("[QuestionService.getQuestionById] START - questionId={}", questionId);

        Question question = findActiveQuestion(questionId);

        log.info("[QuestionService.getQuestionById] SUCCESS - questionId={}", questionId);
        return ApiResponse.ok(ResponseCode.QUESTION_FETCH_SUCCESS, toResponse(question, null));
    }

    // ══════════════════════════════════════════════════════
    // CREATE QUESTION (createdBy/updatedBy auto-set by AuditorAware)
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<QuestionResponse> createQuestion(UUID chapterId, QuestionRequest request, User currentUser) {
        log.info("[QuestionService.createQuestion] START - chapterId={}, type={}, difficulty={}, userId={}",
                chapterId, request.getType(), request.getDifficulty(), currentUser.getId());

        Chapter chapter = findActiveChapter(chapterId);
        validateOptions(request);

        Question question = Question.builder()
                .chapter(chapter)
                .content(request.getContent())
                .type(request.getType())
                .difficulty(request.getDifficulty())
                .explanation(request.getExplanation())
                .build();

        addOptionsToQuestion(question, request.getOptions());
        questionRepository.save(question);

        log.info("[QuestionService.createQuestion] SUCCESS - id={}, chapterId={}, createdBy={}",
                question.getId(), chapterId, currentUser.getId());
        return ApiResponse.ok(ResponseCode.QUESTION_CREATED_SUCCESS, toResponse(question, currentUser));
    }

    // ══════════════════════════════════════════════════════
    // UPDATE QUESTION (owner or admin only)
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<QuestionResponse> updateQuestion(UUID questionId, QuestionRequest request, User currentUser) {
        log.info("[QuestionService.updateQuestion] START - questionId={}, userId={}", questionId, currentUser.getId());

        Question question = findActiveQuestion(questionId);
        checkModifyPermission(question, currentUser);
        validateOptions(request);

        question.setContent(request.getContent());
        question.setType(request.getType());
        question.setDifficulty(request.getDifficulty());
        question.setExplanation(request.getExplanation());

        question.getOptions().clear();
        addOptionsToQuestion(question, request.getOptions());
        questionRepository.save(question);

        log.info("[QuestionService.updateQuestion] SUCCESS - id={}, updatedBy={}", questionId, currentUser.getId());
        return ApiResponse.ok(ResponseCode.QUESTION_UPDATED_SUCCESS, toResponse(question, currentUser));
    }

    // ══════════════════════════════════════════════════════
    // DELETE QUESTION (owner or admin, soft delete)
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<Void> deleteQuestion(UUID questionId, User currentUser) {
        log.info("[QuestionService.deleteQuestion] START - questionId={}, userId={}", questionId, currentUser.getId());

        Question question = findActiveQuestion(questionId);
        checkModifyPermission(question, currentUser);

        question.setIsActive(false);
        questionRepository.save(question);

        log.info("[QuestionService.deleteQuestion] SUCCESS - id={}, softDeleted=true, deletedBy={}", questionId, currentUser.getId());
        return ApiResponse.ok(ResponseCode.QUESTION_DELETED_SUCCESS);
    }

    // ══════════════════════════════════════════════════════
    // TOGGLE SHARE (owner or admin: bật/tắt share cho tất cả giáo viên)
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<QuestionResponse> toggleShare(UUID questionId, User currentUser) {
        log.info("[QuestionService.toggleShare] START - questionId={}, userId={}", questionId, currentUser.getId());

        Question question = findActiveQuestion(questionId);
        checkModifyPermission(question, currentUser);

        boolean newState = !Boolean.TRUE.equals(question.getIsShared());
        question.setIsShared(newState);
        questionRepository.save(question);

        ResponseCode code = newState ? ResponseCode.QUESTION_SHARED_SUCCESS : ResponseCode.QUESTION_UNSHARED_SUCCESS;
        log.info("[QuestionService.toggleShare] SUCCESS - id={}, isShared={}, by={}", questionId, newState, currentUser.getId());
        return ApiResponse.ok(code, toResponse(question, currentUser));
    }

    // ══════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ══════════════════════════════════════════════════════

    private boolean isAdmin(User user) {
        return "ADMIN".equals(user.getRole().getName());
    }

    private boolean isOwner(Question question, User user) {
        return question.getCreatedBy() != null
                && question.getCreatedBy().getId().equals(user.getId());
    }

    private void checkModifyPermission(Question question, User currentUser) {
        if (isAdmin(currentUser)) return;
        if (isOwner(question, currentUser)) return;

        log.warn("[QuestionService.checkModifyPermission] FAILED - userId={} is not owner of questionId={}",
                currentUser.getId(), question.getId());
        throw new BadRequestException(ResponseCode.QUESTION_NOT_AUTHORIZED,
                "Bạn không có quyền thao tác với câu hỏi này");
    }

    private Chapter findActiveChapter(UUID chapterId) {
        return chapterRepository.findByIdAndIsActiveTrue(chapterId)
                .orElseThrow(() -> {
                    log.warn("[QuestionService] FAILED - chapter not found: {}", chapterId);
                    return new ResourceNotFoundException(ResponseCode.CHAPTER_NOT_FOUND);
                });
    }

    private Question findActiveQuestion(UUID questionId) {
        return questionRepository.findByIdAndIsActiveTrue(questionId)
                .orElseThrow(() -> {
                    log.warn("[QuestionService] FAILED - question not found: {}", questionId);
                    return new ResourceNotFoundException(ResponseCode.QUESTION_NOT_FOUND);
                });
    }

    private void validateOptions(QuestionRequest request) {
        long correctCount = request.getOptions().stream()
                .filter(o -> Boolean.TRUE.equals(o.getIsCorrect()))
                .count();

        if (request.getType() == QuestionType.SINGLE_CHOICE && correctCount != 1) {
            log.warn("[QuestionService.validateOptions] FAILED - SINGLE_CHOICE requires exactly 1 correct, got {}", correctCount);
            throw new BadRequestException(ResponseCode.QUESTION_INVALID_CORRECT_ANSWER,
                    "Câu hỏi trắc nghiệm một đáp án phải có đúng 1 đáp án đúng");
        }

        if (request.getType() == QuestionType.MULTI_CHOICE && correctCount < 2) {
            log.warn("[QuestionService.validateOptions] FAILED - MULTI_CHOICE requires at least 2 correct, got {}", correctCount);
            throw new BadRequestException(ResponseCode.QUESTION_INVALID_CORRECT_ANSWER,
                    "Câu hỏi trắc nghiệm nhiều đáp án phải có ít nhất 2 đáp án đúng");
        }
    }

    private void addOptionsToQuestion(Question question, List<OptionRequest> optionRequests) {
        AtomicInteger index = new AtomicInteger(0);
        optionRequests.forEach(optReq -> {
            QuestionOption option = QuestionOption.builder()
                    .question(question)
                    .label(optReq.getLabel())
                    .content(optReq.getContent())
                    .isCorrect(optReq.getIsCorrect())
                    .orderIndex(index.getAndIncrement())
                    .build();
            question.getOptions().add(option);
        });
    }

    private QuestionResponse toResponse(Question question, User currentUser) {
        Chapter chapter = question.getChapter();
        List<OptionResponse> optionResponses = question.getOptions().stream()
                .map(o -> OptionResponse.builder()
                        .id(o.getId())
                        .label(o.getLabel())
                        .content(o.getContent())
                        .isCorrect(o.getIsCorrect())
                        .orderIndex(o.getOrderIndex())
                        .build())
                .toList();

        User creator = question.getCreatedBy();
        User updater = question.getUpdatedBy();

        QuestionResponse.QuestionResponseBuilder builder = QuestionResponse.builder()
                .id(question.getId())
                .chapterId(chapter.getId())
                .chapterName(chapter.getName())
                .subjectId(chapter.getSubject().getId())
                .subjectName(chapter.getSubject().getName())
                .content(question.getContent())
                .type(question.getType())
                .difficulty(question.getDifficulty())
                .explanation(question.getExplanation())
                .options(optionResponses)
                .createdById(creator != null ? creator.getId() : null)
                .createdByName(creator != null ? creator.getFullName() : null)
                .updatedById(updater != null ? updater.getId() : null)
                .updatedByName(updater != null ? updater.getFullName() : null)
                .isShared(question.getIsShared())
                .createdAt(question.getCreatedAt())
                .updatedAt(question.getUpdatedAt());

        if (currentUser != null) {
            builder.isOwner(creator != null && creator.getId().equals(currentUser.getId()));
        }

        return builder.build();
    }
}
