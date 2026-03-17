package com.eduquiz.feature.subject.service;

import com.eduquiz.common.constant.ResponseCode;
import com.eduquiz.common.dto.ApiResponse;
import com.eduquiz.common.dto.PageResponse;
import com.eduquiz.common.exception.DuplicateResourceException;
import com.eduquiz.common.exception.ResourceNotFoundException;
import com.eduquiz.feature.subject.dto.SubjectRequest;
import com.eduquiz.feature.subject.dto.SubjectResponse;
import com.eduquiz.feature.subject.entity.Subject;
import com.eduquiz.feature.subject.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubjectService {

    private final SubjectRepository subjectRepository;

    // ══════════════════════════════════════════════════════
    // GET ALL SUBJECTS (paginated)
    // ══════════════════════════════════════════════════════

    public ApiResponse<PageResponse<SubjectResponse>> getAllSubjects(Pageable pageable) {
        log.info("[SubjectService.getAllSubjects] START - page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());

        Page<SubjectResponse> page = subjectRepository.findAllByIsActiveTrue(pageable)
                .map(this::toResponse);

        log.info("[SubjectService.getAllSubjects] SUCCESS - totalElements={}, totalPages={}", page.getTotalElements(), page.getTotalPages());
        return ApiResponse.ok(ResponseCode.SUBJECT_LIST_SUCCESS, PageResponse.of(page));
    }

    // ══════════════════════════════════════════════════════
    // GET SUBJECT BY ID
    // ══════════════════════════════════════════════════════

    public ApiResponse<SubjectResponse> getSubjectById(UUID id) {
        log.info("[SubjectService.getSubjectById] START - id={}", id);

        Subject subject = subjectRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> {
                    log.warn("[SubjectService.getSubjectById] FAILED - subject not found: {}", id);
                    return new ResourceNotFoundException(ResponseCode.SUBJECT_NOT_FOUND);
                });

        log.info("[SubjectService.getSubjectById] SUCCESS - id={}, name={}", id, subject.getName());
        return ApiResponse.ok(ResponseCode.SUBJECT_FETCH_SUCCESS, toResponse(subject));
    }

    // ══════════════════════════════════════════════════════
    // CREATE SUBJECT
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<SubjectResponse> createSubject(SubjectRequest request) {
        log.info("[SubjectService.createSubject] START - name={}", request.getName());

        if (subjectRepository.existsByNameAndIsActiveTrue(request.getName())) {
            log.warn("[SubjectService.createSubject] FAILED - duplicate name: {}", request.getName());
            throw new DuplicateResourceException(ResponseCode.SUBJECT_NAME_DUPLICATE);
        }

        Subject subject = Subject.builder()
                .name(request.getName())
                .description(request.getDescription())
                .icon(request.getIcon())
                .defaultDurationMinutes(request.getDefaultDurationMinutes())
                .build();
        subjectRepository.save(subject);

        log.info("[SubjectService.createSubject] SUCCESS - id={}, name={}", subject.getId(), subject.getName());
        return ApiResponse.ok(ResponseCode.SUBJECT_CREATED_SUCCESS, toResponse(subject));
    }

    // ══════════════════════════════════════════════════════
    // UPDATE SUBJECT
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<SubjectResponse> updateSubject(UUID id, SubjectRequest request) {
        log.info("[SubjectService.updateSubject] START - id={}, name={}", id, request.getName());

        Subject subject = subjectRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> {
                    log.warn("[SubjectService.updateSubject] FAILED - subject not found: {}", id);
                    return new ResourceNotFoundException(ResponseCode.SUBJECT_NOT_FOUND);
                });

        if (subjectRepository.existsByNameAndIdNotAndIsActiveTrue(request.getName(), id)) {
            log.warn("[SubjectService.updateSubject] FAILED - duplicate name: {}, excludingId={}", request.getName(), id);
            throw new DuplicateResourceException(ResponseCode.SUBJECT_NAME_DUPLICATE);
        }

        subject.setName(request.getName());
        subject.setDescription(request.getDescription());
        subject.setIcon(request.getIcon());
        subject.setDefaultDurationMinutes(request.getDefaultDurationMinutes());
        subjectRepository.save(subject);

        log.info("[SubjectService.updateSubject] SUCCESS - id={}, name={}", id, subject.getName());
        return ApiResponse.ok(ResponseCode.SUBJECT_UPDATED_SUCCESS, toResponse(subject));
    }

    // ══════════════════════════════════════════════════════
    // DELETE SUBJECT (soft delete)
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<Void> deleteSubject(UUID id) {
        log.info("[SubjectService.deleteSubject] START - id={}", id);

        Subject subject = subjectRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> {
                    log.warn("[SubjectService.deleteSubject] FAILED - subject not found: {}", id);
                    return new ResourceNotFoundException(ResponseCode.SUBJECT_NOT_FOUND);
                });

        subject.setIsActive(false);
        subjectRepository.save(subject);

        log.info("[SubjectService.deleteSubject] SUCCESS - id={}, name={}, softDeleted=true", id, subject.getName());
        return ApiResponse.ok(ResponseCode.SUBJECT_DELETED_SUCCESS);
    }

    // ══════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ══════════════════════════════════════════════════════

    private SubjectResponse toResponse(Subject subject) {
        return SubjectResponse.builder()
                .id(subject.getId())
                .name(subject.getName())
                .description(subject.getDescription())
                .icon(subject.getIcon())
                .defaultDurationMinutes(subject.getDefaultDurationMinutes())
                .createdAt(subject.getCreatedAt())
                .updatedAt(subject.getUpdatedAt())
                .build();
    }
}
