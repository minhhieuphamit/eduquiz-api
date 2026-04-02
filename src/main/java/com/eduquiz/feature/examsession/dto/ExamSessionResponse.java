package com.eduquiz.feature.examsession.dto;

import com.eduquiz.feature.examsession.entity.SessionStatus;
import com.eduquiz.feature.question.dto.OptionResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamSessionResponse {
    private UUID id;
    private UUID examId;
    private String examTitle;
    private String subjectName;
    private Integer durationMinutes;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private SessionStatus status;
    private BigDecimal score;
    private Integer correctCount;
    private Integer totalQuestions;

    /** Chỉ khi session IN_PROGRESS — trả về questions + đáp án đã chọn */
    private List<QuestionWithAnswer> questions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionWithAnswer {
        private UUID questionId;
        private String content;
        private List<OptionResponse> options;
        private String selectedAnswer;
    }
}
