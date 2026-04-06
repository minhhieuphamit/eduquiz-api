package com.eduquiz.feature.examsession.repository;

import com.eduquiz.feature.auth.entity.User;
import com.eduquiz.feature.exam.entity.Exam;
import com.eduquiz.feature.examsession.entity.ExamSession;
import com.eduquiz.feature.examsession.entity.SessionStatus;
import com.eduquiz.feature.examroom.entity.ExamRoom;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExamSessionRepository extends JpaRepository<ExamSession, UUID> {

    // Find active (in-progress) session for a user+exam (practice mode, no room)
    Optional<ExamSession> findByUserAndExamAndRoomIsNullAndStatus(
            User user, Exam exam, SessionStatus status);

    // Find active session for a user+exam+room (room mode)
    Optional<ExamSession> findByUserAndExamAndRoomAndStatus(
            User user, Exam exam, ExamRoom room, SessionStatus status);

    // Find any non-expired session (any status) for idempotent start
    Optional<ExamSession> findFirstByUserAndExamAndRoomIsNullOrderByCreatedAtDesc(
            User user, Exam exam);

    Optional<ExamSession> findFirstByUserAndExamAndRoomOrderByCreatedAtDesc(
            User user, Exam exam, ExamRoom room);

    // Paginated history for a student
    Page<ExamSession> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    // Teacher: all sessions for a given exam
    Page<ExamSession> findByExamOrderByCreatedAtDesc(Exam exam, Pageable pageable);

    // Teacher: all sessions for an exam in a room
    Page<ExamSession> findByExamAndRoomOrderByCreatedAtDesc(Exam exam, ExamRoom room, Pageable pageable);

    // All sessions for a given room (for room results view)
    List<ExamSession> findByRoom(ExamRoom room);

    // Find sessions that are IN_PROGRESS and timed out (for auto-submit scheduler)
    @Query("SELECT s FROM ExamSession s WHERE s.status = 'IN_PROGRESS' AND s.endTime <= :now")
    List<ExamSession> findTimedOutSessions(@Param("now") LocalDateTime now);

    // Pessimistic lock for submit to prevent double-submit
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ExamSession s WHERE s.id = :id")
    Optional<ExamSession> findByIdWithLock(@Param("id") UUID id);
}
