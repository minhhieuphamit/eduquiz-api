package com.eduquiz.feature.chapter.service;

import com.eduquiz.common.constant.ResponseCode;
import com.eduquiz.common.dto.ApiResponse;
import com.eduquiz.common.exception.DuplicateResourceException;
import com.eduquiz.common.exception.ResourceNotFoundException;
import com.eduquiz.feature.chapter.dto.ChapterRequest;
import com.eduquiz.feature.chapter.dto.ChapterResponse;
import com.eduquiz.feature.chapter.entity.Chapter;
import com.eduquiz.feature.chapter.repository.ChapterRepository;
import com.eduquiz.feature.question.repository.QuestionRepository;
import com.eduquiz.feature.subject.entity.Subject;
import com.eduquiz.feature.subject.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChapterService {

    private final ChapterRepository chapterRepository;
    private final SubjectRepository subjectRepository;
    private final QuestionRepository questionRepository;

    // ══════════════════════════════════════════════════════
    // GET ALL CHAPTERS BY SUBJECT
    // ══════════════════════════════════════════════════════

    public ApiResponse<List<ChapterResponse>> getChaptersBySubject(UUID subjectId) {
        log.info("[ChapterService.getChaptersBySubject] START - subjectId={}", subjectId);

        Subject subject = findActiveSubject(subjectId);

        List<ChapterResponse> chapters = chapterRepository
                .findBySubjectIdAndIsActiveTrueOrderByOrderIndexAsc(subjectId)
                .stream()
                .map(c -> toResponse(c, subject))
                .toList();

        log.info("[ChapterService.getChaptersBySubject] SUCCESS - subjectId={}, count={}", subjectId, chapters.size());
        return ApiResponse.ok(ResponseCode.CHAPTER_LIST_SUCCESS, chapters);
    }

    // ══════════════════════════════════════════════════════
    // GET CHAPTER BY ID
    // ══════════════════════════════════════════════════════

    public ApiResponse<ChapterResponse> getChapterById(UUID subjectId, UUID chapterId) {
        log.info("[ChapterService.getChapterById] START - subjectId={}, chapterId={}", subjectId, chapterId);

        Subject subject = findActiveSubject(subjectId);
        Chapter chapter = findActiveChapter(chapterId);

        if (!chapter.getSubject().getId().equals(subjectId)) {
            log.warn("[ChapterService.getChapterById] FAILED - chapter {} does not belong to subject {}", chapterId, subjectId);
            throw new ResourceNotFoundException(ResponseCode.CHAPTER_NOT_FOUND);
        }

        log.info("[ChapterService.getChapterById] SUCCESS - chapterId={}, name={}", chapterId, chapter.getName());
        return ApiResponse.ok(ResponseCode.CHAPTER_FETCH_SUCCESS, toResponse(chapter, subject));
    }

    // ══════════════════════════════════════════════════════
    // CREATE CHAPTER
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<ChapterResponse> createChapter(UUID subjectId, ChapterRequest request) {
        log.info("[ChapterService.createChapter] START - subjectId={}, name={}", subjectId, request.getName());

        Subject subject = findActiveSubject(subjectId);

        if (chapterRepository.existsByNameAndSubjectIdAndIsActiveTrue(request.getName(), subjectId)) {
            log.warn("[ChapterService.createChapter] FAILED - duplicate name: {} in subject {}", request.getName(), subjectId);
            throw new DuplicateResourceException(ResponseCode.CHAPTER_NAME_DUPLICATE);
        }

        int orderIndex = request.getOrderIndex() != null
                ? request.getOrderIndex()
                : chapterRepository.findMaxOrderIndexBySubjectId(subjectId) + 1;

        Chapter chapter = Chapter.builder()
                .subject(subject)
                .name(request.getName())
                .description(request.getDescription())
                .orderIndex(orderIndex)
                .build();
        chapterRepository.save(chapter);

        log.info("[ChapterService.createChapter] SUCCESS - id={}, name={}, orderIndex={}", chapter.getId(), chapter.getName(), orderIndex);
        return ApiResponse.ok(ResponseCode.CHAPTER_CREATED_SUCCESS, toResponse(chapter, subject));
    }

    // ══════════════════════════════════════════════════════
    // UPDATE CHAPTER
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<ChapterResponse> updateChapter(UUID subjectId, UUID chapterId, ChapterRequest request) {
        log.info("[ChapterService.updateChapter] START - subjectId={}, chapterId={}, name={}", subjectId, chapterId, request.getName());

        Subject subject = findActiveSubject(subjectId);
        Chapter chapter = findActiveChapter(chapterId);

        if (!chapter.getSubject().getId().equals(subjectId)) {
            throw new ResourceNotFoundException(ResponseCode.CHAPTER_NOT_FOUND);
        }

        if (chapterRepository.existsByNameAndSubjectIdAndIdNotAndIsActiveTrue(request.getName(), subjectId, chapterId)) {
            log.warn("[ChapterService.updateChapter] FAILED - duplicate name: {}", request.getName());
            throw new DuplicateResourceException(ResponseCode.CHAPTER_NAME_DUPLICATE);
        }

        chapter.setName(request.getName());
        chapter.setDescription(request.getDescription());
        if (request.getOrderIndex() != null) {
            chapter.setOrderIndex(request.getOrderIndex());
        }
        chapterRepository.save(chapter);

        log.info("[ChapterService.updateChapter] SUCCESS - id={}, name={}", chapterId, chapter.getName());
        return ApiResponse.ok(ResponseCode.CHAPTER_UPDATED_SUCCESS, toResponse(chapter, subject));
    }

    // ══════════════════════════════════════════════════════
    // DELETE CHAPTER (soft delete)
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<Void> deleteChapter(UUID subjectId, UUID chapterId) {
        log.info("[ChapterService.deleteChapter] START - subjectId={}, chapterId={}", subjectId, chapterId);

        findActiveSubject(subjectId);
        Chapter chapter = findActiveChapter(chapterId);

        if (!chapter.getSubject().getId().equals(subjectId)) {
            throw new ResourceNotFoundException(ResponseCode.CHAPTER_NOT_FOUND);
        }

        chapter.setIsActive(false);
        chapterRepository.save(chapter);

        log.info("[ChapterService.deleteChapter] SUCCESS - id={}, name={}, softDeleted=true", chapterId, chapter.getName());
        return ApiResponse.ok(ResponseCode.CHAPTER_DELETED_SUCCESS);
    }

    // ══════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ══════════════════════════════════════════════════════

    private Subject findActiveSubject(UUID subjectId) {
        return subjectRepository.findByIdAndIsActiveTrue(subjectId)
                .orElseThrow(() -> {
                    log.warn("[ChapterService] FAILED - subject not found: {}", subjectId);
                    return new ResourceNotFoundException(ResponseCode.SUBJECT_NOT_FOUND);
                });
    }

    private Chapter findActiveChapter(UUID chapterId) {
        return chapterRepository.findByIdAndIsActiveTrue(chapterId)
                .orElseThrow(() -> {
                    log.warn("[ChapterService] FAILED - chapter not found: {}", chapterId);
                    return new ResourceNotFoundException(ResponseCode.CHAPTER_NOT_FOUND);
                });
    }

    private ChapterResponse toResponse(Chapter chapter, Subject subject) {
        long questionCount = questionRepository.countByChapterIdAndIsActiveTrue(chapter.getId());
        return ChapterResponse.builder()
                .id(chapter.getId())
                .subjectId(subject.getId())
                .subjectName(subject.getName())
                .name(chapter.getName())
                .description(chapter.getDescription())
                .orderIndex(chapter.getOrderIndex())
                .questionCount(questionCount)
                .createdAt(chapter.getCreatedAt())
                .updatedAt(chapter.getUpdatedAt())
                .build();
    }
}
