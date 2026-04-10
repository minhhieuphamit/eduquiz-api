package com.eduquiz.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamSubmissionEvent {

    private UUID sessionId;
    private UUID userId;
    private UUID examId;
    private UUID subjectId;
    private UUID roomId;          // nullable (practice mode)
    private LocalDateTime submittedAt;
}
