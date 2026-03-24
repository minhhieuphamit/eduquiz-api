package com.eduquiz.feature.question.controller;

import com.eduquiz.common.dto.ApiResponse;
import com.eduquiz.common.dto.PageResponse;
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
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Question", description = "CRUD câu hỏi")
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping("/chapters/{chapterId}/questions")
    @Operation(summary = "Lấy danh sách câu hỏi theo chương (public, phân trang, filter)")
    public ResponseEntity<ApiResponse<PageResponse<QuestionResponse>>> getQuestionsByChapter(
            @PathVariable UUID chapterId,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) QuestionType type,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("[QuestionController.getQuestionsByChapter] START - chapterId={}, difficulty={}, type={}", chapterId, difficulty, type);
        ApiResponse<PageResponse<QuestionResponse>> response = questionService.getQuestionsByChapter(chapterId, difficulty, type, pageable);
        log.info("[QuestionController.getQuestionsByChapter] SUCCESS - code={}", response.getCode());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/questions/{questionId}")
    @Operation(summary = "Lấy chi tiết câu hỏi theo ID (public)")
    public ResponseEntity<ApiResponse<QuestionResponse>> getQuestionById(
            @PathVariable UUID questionId) {
        log.info("[QuestionController.getQuestionById] START - questionId={}", questionId);
        ApiResponse<QuestionResponse> response = questionService.getQuestionById(questionId);
        log.info("[QuestionController.getQuestionById] SUCCESS - code={}", response.getCode());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/chapters/{chapterId}/questions")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Tạo câu hỏi mới (ADMIN, TEACHER)")
    public ResponseEntity<ApiResponse<QuestionResponse>> createQuestion(
            @PathVariable UUID chapterId,
            @Valid @RequestBody QuestionRequest request) {
        log.info("[QuestionController.createQuestion] START - chapterId={}, type={}", chapterId, request.getType());
        ApiResponse<QuestionResponse> response = questionService.createQuestion(chapterId, request);
        log.info("[QuestionController.createQuestion] SUCCESS - code={}", response.getCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/questions/{questionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Cập nhật câu hỏi (ADMIN, TEACHER)")
    public ResponseEntity<ApiResponse<QuestionResponse>> updateQuestion(
            @PathVariable UUID questionId,
            @Valid @RequestBody QuestionRequest request) {
        log.info("[QuestionController.updateQuestion] START - questionId={}", questionId);
        ApiResponse<QuestionResponse> response = questionService.updateQuestion(questionId, request);
        log.info("[QuestionController.updateQuestion] SUCCESS - code={}", response.getCode());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/questions/{questionId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xoá câu hỏi - soft delete (ADMIN)")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(
            @PathVariable UUID questionId) {
        log.info("[QuestionController.deleteQuestion] START - questionId={}", questionId);
        ApiResponse<Void> response = questionService.deleteQuestion(questionId);
        log.info("[QuestionController.deleteQuestion] SUCCESS - code={}", response.getCode());
        return ResponseEntity.ok(response);
    }
}
