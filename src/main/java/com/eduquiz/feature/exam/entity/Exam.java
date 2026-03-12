package com.eduquiz.feature.exam.entity;

/**
 * @Entity exams
 * Fields: id, title, subject(@ManyToOne), durationMinutes, totalQuestions,
 * randomMode(enum), isActive, createdBy(@ManyToOne User), createdAt
 * durationMinutes default lấy từ subject.defaultDurationMinutes
 * TODO: Implement @Entity @Data
 */
public class Exam {
}
