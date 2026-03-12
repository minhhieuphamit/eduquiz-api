package com.eduquiz.feature.examsession.repository;

/**
 * JpaRepository<ExamSession, Long>
 * - findByUserIdOrderByStartedAtDesc(userId) → List (lịch sử)
 * - findByRoomId(roomId) → List (kết quả phòng thi)
 * - findByUserIdAndRoomIdAndStatus(userId, roomId, IN_PROGRESS) → Optional
 * TODO: Implement
 */
public interface ExamSessionRepository {
}
