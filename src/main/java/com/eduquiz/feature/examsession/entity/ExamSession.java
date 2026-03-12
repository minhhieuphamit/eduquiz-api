package com.eduquiz.feature.examsession.entity;

/**
 * @Entity exam_sessions
 * Fields: id, user(@ManyToOne), exam(@ManyToOne), room(@ManyToOne nullable),
 * startedAt, submittedAt, status(enum: IN_PROGRESS/SUBMITTED/GRADED),
 * score, correctCount
 * room = null → luyện tập tự do
 * TODO: @Entity @Data
 */
public class ExamSession {
}
