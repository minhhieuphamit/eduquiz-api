package com.eduquiz.feature.examroom.entity;

/**
 * @Entity exam_rooms
 * Fields: id, roomCode(unique 6 chars), title, exam(@ManyToOne),
 * teacher(@ManyToOne User), randomMode(enum), poolSize,
 * startTime, endTime, status(enum), maxStudents, createdAt
 * <p>
 * Status lifecycle: SCHEDULED → OPEN → IN_PROGRESS → CLOSED
 * TODO: Implement @Entity @Data
 */
public class ExamRoom {
}
