package com.eduquiz.feature.subject.service;

import com.eduquiz.common.constant.ResponseCode;
import com.eduquiz.common.dto.ApiResponse;
import com.eduquiz.common.dto.PageResponse;
import com.eduquiz.common.exception.DuplicateResourceException;
import com.eduquiz.common.exception.ResourceNotFoundException;
import com.eduquiz.common.service.FileStorageService;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final FileStorageService fileStorageService;

    private static final String UPLOAD_SUBFOLDER = "subjects";

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
    public ApiResponse<SubjectResponse> createSubject(SubjectRequest request, MultipartFile image) {
        log.info("[SubjectService.createSubject] START - name={}, hasImage={}", request.getName(), image != null && !image.isEmpty());

        if (subjectRepository.existsByNameAndIsActiveTrue(request.getName())) {
            log.warn("[SubjectService.createSubject] FAILED - duplicate name: {}", request.getName());
            throw new DuplicateResourceException(ResponseCode.SUBJECT_NAME_DUPLICATE);
        }

        String imageRelativePath = null;
        if (image != null && !image.isEmpty()) {
            imageRelativePath = fileStorageService.store(image, UPLOAD_SUBFOLDER);
            log.debug("[SubjectService.createSubject] Image uploaded: {}", imageRelativePath);
        }

        Subject subject = Subject.builder()
                .name(request.getName())
                .description(request.getDescription())
                .imageUrl(imageRelativePath)
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
    public ApiResponse<SubjectResponse> updateSubject(UUID id, SubjectRequest request, MultipartFile image) {
        log.info("[SubjectService.updateSubject] START - id={}, name={}, hasNewImage={}", id, request.getName(), image != null && !image.isEmpty());

        Subject subject = subjectRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> {
                    log.warn("[SubjectService.updateSubject] FAILED - subject not found: {}", id);
                    return new ResourceNotFoundException(ResponseCode.SUBJECT_NOT_FOUND);
                });

        if (subjectRepository.existsByNameAndIdNotAndIsActiveTrue(request.getName(), id)) {
            log.warn("[SubjectService.updateSubject] FAILED - duplicate name: {}, excludingId={}", request.getName(), id);
            throw new DuplicateResourceException(ResponseCode.SUBJECT_NAME_DUPLICATE);
        }

        // Upload ảnh mới → xoá ảnh cũ
        if (image != null && !image.isEmpty()) {
            String oldImagePath = subject.getImageUrl();
            String newImagePath = fileStorageService.store(image, UPLOAD_SUBFOLDER);
            subject.setImageUrl(newImagePath);
            log.debug("[SubjectService.updateSubject] Image replaced: old={}, new={}", oldImagePath, newImagePath);

            // Xoá ảnh cũ sau khi upload thành công
            if (oldImagePath != null) {
                fileStorageService.delete(oldImagePath);
            }
        }

        subject.setName(request.getName());
        subject.setDescription(request.getDescription());
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

        // Không xoá file ảnh khi soft delete (có thể restore lại)
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
                .imageUrl(fileStorageService.toFullUrl(subject.getImageUrl()))
                .defaultDurationMinutes(subject.getDefaultDurationMinutes())
                .createdAt(subject.getCreatedAt())
                .updatedAt(subject.getUpdatedAt())
                .build();
    }
}
