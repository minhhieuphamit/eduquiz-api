package com.eduquiz.feature.examsession.dto;

import com.eduquiz.feature.examsession.entity.SessionStatus;
import com.eduquiz.feature.examsession.entity.SubmissionSource;
import com.eduquiz.feature.question.entity.QuestionType;
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
    private UUID examId;
    private String examTitle;
    private String subjectName;
    private String studentName;

    private SessionStatus status;
    private SubmissionSource submissionSource;

    // Score summary
    private BigDecimal score;          // percentage 0.00 – 100.00
    private Integer correctCount;
    private Integer incorrectCount;
    private Integer unansweredCount;
    private Integer totalQuestions;

    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;

    // Per-question breakdown (populated for detail view)
    private List<AnswerDetail> answers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerDetail {
        private UUID questionId;
        private String questionContent;
        private QuestionType questionType;
        private List<OptionDetail> options;
        private List<String> selectedOptions; // what student chose
        private List<String> correctOptions;  // what was correct
        private Boolean isCorrect;
        private String explanation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionDetail {
        private String label;
        private String content;
        private Boolean isCorrect; // revealed in result
    }
}
