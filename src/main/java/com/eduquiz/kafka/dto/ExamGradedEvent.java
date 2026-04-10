package com.eduquiz.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamGradedEvent {

    private UUID sessionId;
    private UUID userId;
    private UUID examId;
    private UUID subjectId;
    private UUID roomId;          // nullable
    private BigDecimal score;
    private int correctCount;
    private int totalQuestions;
    private LocalDateTime gradedAt;
}
