package com.eduquiz.feature.exam.dto;

import com.eduquiz.feature.exam.entity.ExamType;
import com.eduquiz.feature.exam.entity.RandomMode;
import com.eduquiz.feature.question.dto.QuestionResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamResponse {
    private UUID id;
    private String title;
    private String description;
    private UUID subjectId;
    private String subjectName;
    private Integer durationMinutes;
    private Integer totalQuestions;
    private RandomMode randomMode;
    private Integer year;
    private ExamType examType;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Chỉ khi xem chi tiết
    private List<QuestionResponse> questions;
}
