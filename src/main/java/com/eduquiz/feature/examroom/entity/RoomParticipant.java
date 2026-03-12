package com.eduquiz.feature.examroom.entity;

/**
 * @Entity room_participants
 * Fields: id, room(@ManyToOne ExamRoom), user(@ManyToOne User),
 * assignedExam(@ManyToOne Exam), joinedAt,
 * status(enum: JOINED/IN_PROGRESS/SUBMITTED)
 * TODO: Implement @Entity @Data
 */
public class RoomParticipant {
}
