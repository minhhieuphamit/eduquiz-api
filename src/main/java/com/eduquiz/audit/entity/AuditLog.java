package com.eduquiz.audit.entity;

/**
 * @Entity audit_logs
 * Fields: id, userId, action(VARCHAR), detail(JSONB → Map<String,Object>), createdAt
 * Dùng @JdbcTypeCode(SqlTypes.JSON) cho JSONB column
 * TODO: @Entity @Data
 */
public class AuditLog {
}
