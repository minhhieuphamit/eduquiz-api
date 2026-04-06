package com.eduquiz.feature.examsession.dto;

import com.eduquiz.feature.examsession.entity.SessionStatus;
import com.eduquiz.feature.question.entity.QuestionType;
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
public class ExamSessionResponse {

    private UUID id;
    private UUID examId;
    private String examTitle;
    private String subjectName;
    private Integer durationMinutes;

    private LocalDateTime startedAt;
    private LocalDateTime expiresAt;
    private Long remainingSeconds;

    private SessionStatus status;
    private Integer totalQuestions;

    // Questions + current saved answers (for resume/reload)
    private List<QuestionWithAnswer> questions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionWithAnswer {
        private UUID questionId;
        private Integer orderIndex;
        private String content;
        private QuestionType type;
        private List<OptionInfo> options;
        private String selectedAnswer; // null = not yet answered
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionInfo {
        private UUID optionId;
        private String label;
        private String content;
        // Note: isCorrect intentionally omitted — do NOT leak to students
    }
}
