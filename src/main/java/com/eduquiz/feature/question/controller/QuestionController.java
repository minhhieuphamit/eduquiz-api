package com.eduquiz.feature.question.controller;

import com.eduquiz.common.dto.ApiResponse;
import com.eduquiz.common.dto.PageResponse;
import com.eduquiz.feature.auth.entity.User;
import com.eduquiz.feature.question.dto.QuestionRequest;
import com.eduquiz.feature.question.dto.QuestionResponse;
import com.eduquiz.feature.question.entity.Difficulty;
import com.eduquiz.feature.question.entity.QuestionType;
import com.eduquiz.feature.question.service.QuestionService;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Question", description = "CRUD câu hỏi + share")
public class QuestionController {

    private final QuestionService questionService;

    // ══════════════════════════════════════════════════════
    // PUBLIC ENDPOINTS
    // ══════════════════════════════════════════════════════

    @GetMapping("/chapters/{chapterId}/questions")
    @Operation(summary = "Lấy danh sách câu hỏi theo chương (public)")
    public ResponseEntity<ApiResponse<PageResponse<QuestionResponse>>> getQuestionsByChapter(
            @PathVariable UUID chapterId,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) QuestionType type,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("[QuestionController.getQuestionsByChapter] START - chapterId={}", chapterId);
        return ResponseEntity.ok(questionService.getQuestionsByChapter(chapterId, difficulty, type, pageable));
    }

    @GetMapping("/questions/{questionId}")
    @Operation(summary = "Lấy chi tiết câu hỏi theo ID (public)")
    public ResponseEntity<ApiResponse<QuestionResponse>> getQuestionById(@PathVariable UUID questionId) {
        log.info("[QuestionController.getQuestionById] START - questionId={}", questionId);
        return ResponseEntity.ok(questionService.getQuestionById(questionId));
    }

    // ══════════════════════════════════════════════════════
    // MY QUESTIONS (role-based visibility)
    // ══════════════════════════════════════════════════════

    @GetMapping("/questions/my")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Câu hỏi của tôi: ADMIN thấy tất cả, TEACHER thấy câu mình tạo + câu được share")
    public ResponseEntity<ApiResponse<PageResponse<QuestionResponse>>> getMyQuestions(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) UUID chapterId,
            @RequestParam(required = false) UUID subjectId,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) QuestionType type,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("[QuestionController.getMyQuestions] START - userId={}, role={}", currentUser.getId(), currentUser.getRole().getName());
        return ResponseEntity.ok(questionService.getMyQuestions(currentUser, chapterId, subjectId, difficulty, type, keyword, pageable));
    }

    // ══════════════════════════════════════════════════════
    // CRUD
    // ══════════════════════════════════════════════════════

    @PostMapping("/chapters/{chapterId}/questions")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Tạo câu hỏi mới (ADMIN, TEACHER)")
    public ResponseEntity<ApiResponse<QuestionResponse>> createQuestion(
            @PathVariable UUID chapterId,
            @Valid @RequestBody QuestionRequest request,
            @AuthenticationPrincipal User currentUser) {
        log.info("[QuestionController.createQuestion] START - chapterId={}, userId={}", chapterId, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(questionService.createQuestion(chapterId, request, currentUser));
    }

    @PutMapping("/questions/{questionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Cập nhật câu hỏi (owner hoặc ADMIN)")
    public ResponseEntity<ApiResponse<QuestionResponse>> updateQuestion(
            @PathVariable UUID questionId,
            @Valid @RequestBody QuestionRequest request,
            @AuthenticationPrincipal User currentUser) {
        log.info("[QuestionController.updateQuestion] START - questionId={}, userId={}", questionId, currentUser.getId());
        return ResponseEntity.ok(questionService.updateQuestion(questionId, request, currentUser));
    }

    @DeleteMapping("/questions/{questionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Xoá câu hỏi - soft delete (owner hoặc ADMIN)")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(
            @PathVariable UUID questionId,
            @AuthenticationPrincipal User currentUser) {
        log.info("[QuestionController.deleteQuestion] START - questionId={}, userId={}", questionId, currentUser.getId());
        return ResponseEntity.ok(questionService.deleteQuestion(questionId, currentUser));
    }

    // ══════════════════════════════════════════════════════
    // TOGGLE SHARE
    // ══════════════════════════════════════════════════════

    @PutMapping("/questions/{questionId}/share")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Bật/tắt chia sẻ câu hỏi cho tất cả giáo viên (owner hoặc ADMIN)")
    public ResponseEntity<ApiResponse<QuestionResponse>> toggleShare(
            @PathVariable UUID questionId,
            @AuthenticationPrincipal User currentUser) {
        log.info("[QuestionController.toggleShare] START - questionId={}, userId={}", questionId, currentUser.getId());
        return ResponseEntity.ok(questionService.toggleShare(questionId, currentUser));
    }
}
