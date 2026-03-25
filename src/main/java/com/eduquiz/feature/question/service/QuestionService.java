package com.eduquiz.feature.question.service;

import com.eduquiz.common.constant.ResponseCode;
import com.eduquiz.common.dto.ApiResponse;
import com.eduquiz.common.dto.PageResponse;
import com.eduquiz.common.exception.BadRequestException;
import com.eduquiz.common.exception.ResourceNotFoundException;
import com.eduquiz.feature.chapter.entity.Chapter;
import com.eduquiz.feature.chapter.repository.ChapterRepository;
import com.eduquiz.feature.question.dto.*;
import com.eduquiz.feature.question.entity.Difficulty;
import com.eduquiz.feature.question.entity.Question;
import com.eduquiz.feature.question.entity.QuestionOption;
import com.eduquiz.feature.question.entity.QuestionType;
import com.eduquiz.feature.question.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    // GET QUESTIONS BY CHAPTER (paginated + filter)
    // ══════════════════════════════════════════════════════

    public ApiResponse<PageResponse<QuestionResponse>> getQuestionsByChapter(
            UUID chapterId, Difficulty difficulty, QuestionType type, Pageable pageable) {
        log.info("[QuestionService.getQuestionsByChapter] START - chapterId={}, difficulty={}, type={}", chapterId, difficulty, type);

        findActiveChapter(chapterId);

        Page<Question> page;
        if (difficulty != null && type != null) {
            page = questionRepository.findByChapterIdAndDifficultyAndTypeAndIsActiveTrue(chapterId, difficulty, type, pageable);
        } else if (difficulty != null) {
            page = questionRepository.findByChapterIdAndDifficultyAndIsActiveTrue(chapterId, difficulty, pageable);
        } else if (type != null) {
            page = questionRepository.findByChapterIdAndTypeAndIsActiveTrue(chapterId, type, pageable);
        } else {
            page = questionRepository.findByChapterIdAndIsActiveTrue(chapterId, pageable);
        }

        Page<QuestionResponse> responsePage = page.map(this::toResponse);

        log.info("[QuestionService.getQuestionsByChapter] SUCCESS - chapterId={}, totalElements={}", chapterId, page.getTotalElements());
        return ApiResponse.ok(ResponseCode.QUESTION_LIST_SUCCESS, PageResponse.of(responsePage));
    }

    // ══════════════════════════════════════════════════════
    // GET QUESTION BY ID
    // ══════════════════════════════════════════════════════

    public ApiResponse<QuestionResponse> getQuestionById(UUID questionId) {
        log.info("[QuestionService.getQuestionById] START - questionId={}", questionId);

        Question question = findActiveQuestion(questionId);

        log.info("[QuestionService.getQuestionById] SUCCESS - questionId={}", questionId);
        return ApiResponse.ok(ResponseCode.QUESTION_FETCH_SUCCESS, toResponse(question));
    }

    // ══════════════════════════════════════════════════════
    // CREATE QUESTION
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<QuestionResponse> createQuestion(UUID chapterId, QuestionRequest request) {
        log.info("[QuestionService.createQuestion] START - chapterId={}, type={}, difficulty={}", chapterId, request.getType(), request.getDifficulty());

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

        log.info("[QuestionService.createQuestion] SUCCESS - id={}, chapterId={}", question.getId(), chapterId);
        return ApiResponse.ok(ResponseCode.QUESTION_CREATED_SUCCESS, toResponse(question));
    }

    // ══════════════════════════════════════════════════════
    // UPDATE QUESTION
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<QuestionResponse> updateQuestion(UUID questionId, QuestionRequest request) {
        log.info("[QuestionService.updateQuestion] START - questionId={}", questionId);

        Question question = findActiveQuestion(questionId);
        validateOptions(request);

        question.setContent(request.getContent());
        question.setType(request.getType());
        question.setDifficulty(request.getDifficulty());
        question.setExplanation(request.getExplanation());

        // Replace options
        question.getOptions().clear();
        addOptionsToQuestion(question, request.getOptions());
        questionRepository.save(question);

        log.info("[QuestionService.updateQuestion] SUCCESS - id={}", questionId);
        return ApiResponse.ok(ResponseCode.QUESTION_UPDATED_SUCCESS, toResponse(question));
    }

    // ══════════════════════════════════════════════════════
    // DELETE QUESTION (soft delete)
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<Void> deleteQuestion(UUID questionId) {
        log.info("[QuestionService.deleteQuestion] START - questionId={}", questionId);

        Question question = findActiveQuestion(questionId);
        question.setIsActive(false);
        questionRepository.save(question);

        log.info("[QuestionService.deleteQuestion] SUCCESS - id={}, softDeleted=true", questionId);
        return ApiResponse.ok(ResponseCode.QUESTION_DELETED_SUCCESS);
    }

    // ══════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ══════════════════════════════════════════════════════

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
            log.warn("[QuestionService.validateOptions] FAILED - SINGLE_CHOICE must have exactly 1 correct answer, got {}", correctCount);
            throw new BadRequestException(ResponseCode.QUESTION_INVALID_CORRECT_ANSWER,
                    "Câu hỏi trắc nghiệm một đáp án phải có đúng 1 đáp án đúng");
        }

        if (request.getType() == QuestionType.MULTI_CHOICE && correctCount < 2) {
            log.warn("[QuestionService.validateOptions] FAILED - MULTI_CHOICE must have at least 2 correct answers, got {}", correctCount);
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

    private QuestionResponse toResponse(Question question) {
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

        return QuestionResponse.builder()
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
                .createdAt(question.getCreatedAt())
                .updatedAt(question.getUpdatedAt())
                .build();
    }
}
