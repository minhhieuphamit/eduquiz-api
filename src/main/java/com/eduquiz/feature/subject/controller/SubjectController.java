package com.eduquiz.feature.subject.controller;

import com.eduquiz.common.dto.ApiResponse;
import com.eduquiz.common.dto.PageResponse;
import com.eduquiz.feature.subject.dto.SubjectRequest;
import com.eduquiz.feature.subject.dto.SubjectResponse;
import com.eduquiz.feature.subject.service.SubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subjects")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Subject", description = "CRUD môn học")
public class SubjectController {

    private final SubjectService subjectService;

    @GetMapping
    @Operation(summary = "Lấy danh sách môn học (public, có phân trang)")
    public ResponseEntity<ApiResponse<PageResponse<SubjectResponse>>> getAllSubjects(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        log.info("[SubjectController.getAllSubjects] START - page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        ApiResponse<PageResponse<SubjectResponse>> response = subjectService.getAllSubjects(pageable);
        log.info("[SubjectController.getAllSubjects] SUCCESS - code={}", response.getCode());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết môn học theo ID (public)")
    public ResponseEntity<ApiResponse<SubjectResponse>> getSubjectById(@PathVariable UUID id) {
        log.info("[SubjectController.getSubjectById] START - id={}", id);
        ApiResponse<SubjectResponse> response = subjectService.getSubjectById(id);
        log.info("[SubjectController.getSubjectById] SUCCESS - id={}, code={}", id, response.getCode());
        return ResponseEntity.ok(response);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Tạo môn học mới (ADMIN, TEACHER) - multipart/form-data")
    public ResponseEntity<ApiResponse<SubjectResponse>> createSubject(
            @Valid @RequestPart("data") SubjectRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        log.info("[SubjectController.createSubject] START - name={}, hasImage={}", request.getName(), image != null && !image.isEmpty());
        ApiResponse<SubjectResponse> response = subjectService.createSubject(request, image);
        log.info("[SubjectController.createSubject] SUCCESS - code={}", response.getCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Cập nhật môn học (ADMIN, TEACHER) - multipart/form-data")
    public ResponseEntity<ApiResponse<SubjectResponse>> updateSubject(
            @PathVariable UUID id,
            @Valid @RequestPart("data") SubjectRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        log.info("[SubjectController.updateSubject] START - id={}, name={}, hasNewImage={}", id, request.getName(), image != null && !image.isEmpty());
        ApiResponse<SubjectResponse> response = subjectService.updateSubject(id, request, image);
        log.info("[SubjectController.updateSubject] SUCCESS - id={}, code={}", id, response.getCode());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Xoá môn học - soft delete (ADMIN, TEACHER)")
    public ResponseEntity<ApiResponse<Void>> deleteSubject(@PathVariable UUID id) {
        log.info("[SubjectController.deleteSubject] START - id={}", id);
        ApiResponse<Void> response = subjectService.deleteSubject(id);
        log.info("[SubjectController.deleteSubject] SUCCESS - id={}, code={}", id, response.getCode());
        return ResponseEntity.ok(response);
    }
}
