package com.eduquiz.feature.examsession.controller;

import com.eduquiz.common.dto.ApiResponse;
import com.eduquiz.common.dto.PageResponse;
import com.eduquiz.feature.auth.entity.User;
import com.eduquiz.feature.examsession.dto.*;
import com.eduquiz.feature.examsession.service.ExamSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exam-sessions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Exam Session", description = "Làm bài thi")
public class ExamSessionController {

    private final ExamSessionService examSessionService;

    @PostMapping("/start")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Bắt đầu làm bài (STUDENT)")
    public ResponseEntity<ApiResponse<ExamSessionResponse>> startExam(
            @Valid @RequestBody StartExamRequest request,
            @AuthenticationPrincipal User student) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(examSessionService.startExam(request, student));
    }

    @PutMapping("/{id}/answer")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Lưu đáp án (auto-save)")
    public ResponseEntity<ApiResponse<Void>> saveAnswer(
            @PathVariable UUID id,
            @Valid @RequestBody AnswerRequest request,
            @AuthenticationPrincipal User student) {
        return ResponseEntity.ok(examSessionService.saveAnswer(id, request, student));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Nộp bài thi")
    public ResponseEntity<ApiResponse<ExamSessionResponse>> submitExam(
            @PathVariable UUID id,
            @AuthenticationPrincipal User student) {
        return ResponseEntity.ok(examSessionService.submitExam(id, student));
    }

    @GetMapping("/{id}/result")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Xem kết quả")
    public ResponseEntity<ApiResponse<ExamResultResponse>> getResult(
            @PathVariable UUID id,
            @AuthenticationPrincipal User student) {
        return ResponseEntity.ok(examSessionService.getResult(id, student));
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Lịch sử làm bài")
    public ResponseEntity<ApiResponse<PageResponse<ExamSessionResponse>>> getHistory(
            @AuthenticationPrincipal User student,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(examSessionService.getHistory(student, pageable));
    }
}
