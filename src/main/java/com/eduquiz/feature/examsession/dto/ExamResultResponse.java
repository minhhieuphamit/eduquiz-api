package com.eduquiz.feature.examsession.dto;

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
public class ExamResultResponse {

    private UUID sessionId;
    private String examTitle;
    private String subjectName;
    private BigDecimal score;
    private int correctCount;
    private int totalQuestions;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;

    private List<AnswerDetail> answers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerDetail {
        private UUID questionId;
        private String content;
        private String optionA;
        private String optionB;
        private String optionC;
        private String optionD;
        private String selectedAnswer;
        private String correctAnswer;
        private boolean isCorrect;
        private String explanation;
    }
}
