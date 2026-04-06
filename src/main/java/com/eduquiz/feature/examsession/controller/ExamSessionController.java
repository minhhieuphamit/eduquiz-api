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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exam-sessions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Exam Session", description = "Luồng học sinh làm bài thi")
public class ExamSessionController {

    private final ExamSessionService examSessionService;

    // ── Start / Resume ──────────────────────────────────────────────────────

    @PostMapping("/start")
    @Operation(summary = "Bắt đầu / resume bài thi (idempotent)")
    public ResponseEntity<ApiResponse<ExamSessionResponse>> startExam(
            @Valid @RequestBody StartExamRequest request,
            @AuthenticationPrincipal User currentUser) {
        log.info("[ExamSessionController.start] userId={}, examId={}", currentUser.getId(), request.getExamId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(examSessionService.startExam(request, currentUser));
    }

    // ── Get Session (by ID) ─────────────────────────────────────────────────

    @GetMapping("/{sessionId}")
    @Operation(summary = "Lấy thông tin phiên làm bài (owner / teacher)")
    public ResponseEntity<ApiResponse<ExamSessionResponse>> getSession(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal User currentUser) {
        log.info("[ExamSessionController.getSession] sessionId={}", sessionId);
        return ResponseEntity.ok(examSessionService.getSession(sessionId, currentUser));
    }

    // ── Get Current In-Progress Session (resume) ────────────────────────────

    @GetMapping("/current")
    @Operation(summary = "Lấy phiên đang làm dở (reload / resume)")
    public ResponseEntity<ApiResponse<ExamSessionResponse>> getCurrentSession(
            @RequestParam UUID examId,
            @RequestParam(required = false) UUID roomId,
            @AuthenticationPrincipal User currentUser) {
        log.info("[ExamSessionController.getCurrent] examId={}, roomId={}", examId, roomId);
        return ResponseEntity.ok(examSessionService.getCurrentSession(examId, roomId, currentUser));
    }

    // ── Save Single Answer ──────────────────────────────────────────────────

    @PutMapping("/{sessionId}/answers")
    @Operation(summary = "Lưu đáp án cho 1 câu hỏi (autosave)")
    public ResponseEntity<ApiResponse<Void>> saveAnswer(
            @PathVariable UUID sessionId,
            @Valid @RequestBody AnswerRequest request,
            @AuthenticationPrincipal User currentUser) {
        log.debug("[ExamSessionController.saveAnswer] sessionId={}, questionId={}", sessionId, request.getQuestionId());
        return ResponseEntity.ok(examSessionService.saveAnswer(sessionId, request, currentUser));
    }

    // ── Batch Save Answers ──────────────────────────────────────────────────

    @PutMapping("/{sessionId}/answers:batch")
    @Operation(summary = "Lưu nhiều đáp án cùng lúc (autosave batch)")
    public ResponseEntity<ApiResponse<Void>> batchSaveAnswers(
            @PathVariable UUID sessionId,
            @Valid @RequestBody BatchAnswerRequest request,
            @AuthenticationPrincipal User currentUser) {
        log.debug("[ExamSessionController.batchSave] sessionId={}, count={}", sessionId, request.getAnswers().size());
        return ResponseEntity.ok(examSessionService.batchSaveAnswers(sessionId, request, currentUser));
    }

    // ── Submit ──────────────────────────────────────────────────────────────

    @PostMapping("/{sessionId}/submit")
    @Operation(summary = "Nộp bài thi (grading inline)")
    public ResponseEntity<ApiResponse<ExamResultResponse>> submitExam(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal User currentUser) {
        log.info("[ExamSessionController.submit] sessionId={}, userId={}", sessionId, currentUser.getId());
        return ResponseEntity.ok(examSessionService.submitExam(sessionId, currentUser));
    }

    // ── Get Result ──────────────────────────────────────────────────────────

    @GetMapping("/{sessionId}/result")
    @Operation(summary = "Xem kết quả bài thi (owner / teacher)")
    public ResponseEntity<ApiResponse<ExamResultResponse>> getResult(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal User currentUser) {
        log.info("[ExamSessionController.getResult] sessionId={}", sessionId);
        return ResponseEntity.ok(examSessionService.getResult(sessionId, currentUser));
    }

    // ── History ─────────────────────────────────────────────────────────────

    @GetMapping("/history")
    @Operation(summary = "Lịch sử làm bài của học sinh (paginated)")
    public ResponseEntity<ApiResponse<PageResponse<ExamResultResponse>>> getHistory(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal User currentUser) {
        log.info("[ExamSessionController.history] userId={}", currentUser.getId());
        return ResponseEntity.ok(examSessionService.getHistory(currentUser, pageable));
    }
}
