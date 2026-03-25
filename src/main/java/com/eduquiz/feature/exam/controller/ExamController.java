package com.eduquiz.feature.exam.controller;

import com.eduquiz.common.dto.ApiResponse;
import com.eduquiz.common.dto.PageResponse;
import com.eduquiz.feature.exam.dto.CreateExamRequest;
import com.eduquiz.feature.exam.dto.ExamResponse;
import com.eduquiz.feature.exam.entity.ExamType;
import com.eduquiz.feature.exam.service.ExamService;
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
@Tag(name = "Exam", description = "CRUD đề thi")
public class ExamController {

    private final ExamService examService;

    @GetMapping("/exams")
    @Operation(summary = "Lấy tất cả đề thi (public, phân trang)")
    public ResponseEntity<ApiResponse<PageResponse<ExamResponse>>> getAllExams(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("[ExamController.getAllExams] START");
        ApiResponse<PageResponse<ExamResponse>> response = examService.getAllExams(pageable);
        log.info("[ExamController.getAllExams] SUCCESS - code={}", response.getCode());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/subjects/{subjectId}/exams")
    @Operation(summary = "Lấy đề thi theo môn học (public, filter theo examType/year)")
    public ResponseEntity<ApiResponse<PageResponse<ExamResponse>>> getExamsBySubject(
            @PathVariable UUID subjectId,
            @RequestParam(required = false) ExamType examType,
            @RequestParam(required = false) Integer year,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("[ExamController.getExamsBySubject] START - subjectId={}, examType={}, year={}", subjectId, examType, year);
        ApiResponse<PageResponse<ExamResponse>> response = examService.getExamsBySubject(subjectId, examType, year, pageable);
        log.info("[ExamController.getExamsBySubject] SUCCESS - code={}", response.getCode());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/exams/{examId}")
    @Operation(summary = "Lấy chi tiết đề thi + danh sách câu hỏi (public)")
    public ResponseEntity<ApiResponse<ExamResponse>> getExamById(@PathVariable UUID examId) {
        log.info("[ExamController.getExamById] START - examId={}", examId);
        ApiResponse<ExamResponse> response = examService.getExamById(examId);
        log.info("[ExamController.getExamById] SUCCESS - code={}", response.getCode());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/exams")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Tạo đề thi mới (ADMIN, TEACHER)")
    public ResponseEntity<ApiResponse<ExamResponse>> createExam(
            @Valid @RequestBody CreateExamRequest request) {
        log.info("[ExamController.createExam] START - title={}, randomMode={}", request.getTitle(), request.getRandomMode());
        ApiResponse<ExamResponse> response = examService.createExam(request);
        log.info("[ExamController.createExam] SUCCESS - code={}", response.getCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/exams/{examId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Cập nhật đề thi (ADMIN, TEACHER)")
    public ResponseEntity<ApiResponse<ExamResponse>> updateExam(
            @PathVariable UUID examId,
            @Valid @RequestBody CreateExamRequest request) {
        log.info("[ExamController.updateExam] START - examId={}", examId);
        ApiResponse<ExamResponse> response = examService.updateExam(examId, request);
        log.info("[ExamController.updateExam] SUCCESS - code={}", response.getCode());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/exams/{examId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xoá đề thi - soft delete (ADMIN)")
    public ResponseEntity<ApiResponse<Void>> deleteExam(@PathVariable UUID examId) {
        log.info("[ExamController.deleteExam] START - examId={}", examId);
        ApiResponse<Void> response = examService.deleteExam(examId);
        log.info("[ExamController.deleteExam] SUCCESS - code={}", response.getCode());
        return ResponseEntity.ok(response);
    }
}
