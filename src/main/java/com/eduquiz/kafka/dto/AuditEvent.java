package com.eduquiz.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {

    private UUID userId;          // nullable (anonymous actions)
    private String action;        // LOGIN, REGISTER, START_EXAM, SUBMIT_EXAM, CREATE_ROOM, JOIN_ROOM, ...
    private String entityName;    // "ExamSession", "ExamRoom", ... (nullable)
    private UUID entityId;        // nullable
    private Map<String, Object> detail;
    private LocalDateTime timestamp;
}
