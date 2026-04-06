package com.eduquiz.feature.examsession.entity;

public enum SubmissionSource {
    MANUAL,       // Student clicked submit
    AUTO_TIMEOUT, // System auto-submitted when time ran out
    SYSTEM        // Admin/system forced submission
}
