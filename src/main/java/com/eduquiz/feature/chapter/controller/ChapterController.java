package com.eduquiz.feature.chapter.controller;

import com.eduquiz.common.dto.ApiResponse;
import com.eduquiz.feature.chapter.dto.ChapterRequest;
import com.eduquiz.feature.chapter.dto.ChapterResponse;
import com.eduquiz.feature.chapter.service.ChapterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subjects/{subjectId}/chapters")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Chapter", description = "CRUD chương theo môn học")
public class ChapterController {

    private final ChapterService chapterService;

    @GetMapping
    @Operation(summary = "Lấy danh sách chương theo môn học (public)")
    public ResponseEntity<ApiResponse<List<ChapterResponse>>> getChaptersBySubject(
            @PathVariable UUID subjectId) {
        log.info("[ChapterController.getChaptersBySubject] START - subjectId={}", subjectId);
        ApiResponse<List<ChapterResponse>> response = chapterService.getChaptersBySubject(subjectId);
        log.info("[ChapterController.getChaptersBySubject] SUCCESS - code={}", response.getCode());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{chapterId}")
    @Operation(summary = "Lấy chi tiết chương theo ID (public)")
    public ResponseEntity<ApiResponse<ChapterResponse>> getChapterById(
            @PathVariable UUID subjectId,
            @PathVariable UUID chapterId) {
        log.info("[ChapterController.getChapterById] START - subjectId={}, chapterId={}", subjectId, chapterId);
        ApiResponse<ChapterResponse> response = chapterService.getChapterById(subjectId, chapterId);
        log.info("[ChapterController.getChapterById] SUCCESS - code={}", response.getCode());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Tạo chương mới (ADMIN, TEACHER)")
    public ResponseEntity<ApiResponse<ChapterResponse>> createChapter(
            @PathVariable UUID subjectId,
            @Valid @RequestBody ChapterRequest request) {
        log.info("[ChapterController.createChapter] START - subjectId={}, name={}", subjectId, request.getName());
        ApiResponse<ChapterResponse> response = chapterService.createChapter(subjectId, request);
        log.info("[ChapterController.createChapter] SUCCESS - code={}", response.getCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{chapterId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Cập nhật chương (ADMIN, TEACHER)")
    public ResponseEntity<ApiResponse<ChapterResponse>> updateChapter(
            @PathVariable UUID subjectId,
            @PathVariable UUID chapterId,
            @Valid @RequestBody ChapterRequest request) {
        log.info("[ChapterController.updateChapter] START - subjectId={}, chapterId={}", subjectId, chapterId);
        ApiResponse<ChapterResponse> response = chapterService.updateChapter(subjectId, chapterId, request);
        log.info("[ChapterController.updateChapter] SUCCESS - code={}", response.getCode());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{chapterId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xoá chương - soft delete (ADMIN)")
    public ResponseEntity<ApiResponse<Void>> deleteChapter(
            @PathVariable UUID subjectId,
            @PathVariable UUID chapterId) {
        log.info("[ChapterController.deleteChapter] START - subjectId={}, chapterId={}", subjectId, chapterId);
        ApiResponse<Void> response = chapterService.deleteChapter(subjectId, chapterId);
        log.info("[ChapterController.deleteChapter] SUCCESS - code={}", response.getCode());
        return ResponseEntity.ok(response);
    }
}
