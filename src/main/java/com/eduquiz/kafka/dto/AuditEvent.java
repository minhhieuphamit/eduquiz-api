package com.eduquiz.kafka.dto;

/**
 * Audit event.
 * Fields: userId, action(String), detail(Map<String,Object>), timestamp
 * Actions: LOGIN, REGISTER, START_EXAM, SUBMIT_EXAM, CREATE_QUESTION,
 * CREATE_EXAM, CREATE_ROOM, JOIN_ROOM...
 * TODO: @Data @Builder
 */
public class AuditEvent {
}
