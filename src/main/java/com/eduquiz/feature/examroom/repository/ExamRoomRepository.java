package com.eduquiz.feature.examroom.repository;

/**
 * JpaRepository<ExamRoom, Long>
 * - findByRoomCode(code) → Optional
 * - findByTeacherIdOrderByCreatedAtDesc(teacherId) → List
 * - findByStatusAndStartTimeBefore(SCHEDULED, now) → rooms cần mở
 * - findByStatusAndEndTimeBefore(OPEN/IN_PROGRESS, now) → rooms cần đóng
 * TODO: Implement
 */
public interface ExamRoomRepository {
}
