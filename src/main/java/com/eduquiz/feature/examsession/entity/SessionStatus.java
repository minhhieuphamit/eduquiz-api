package com.eduquiz.feature.examsession.entity;

public enum SessionStatus {
    IN_PROGRESS,
    SUBMITTED,        // Student manually submitted
    AUTO_SUBMITTED    // System auto-submitted on timeout
}
