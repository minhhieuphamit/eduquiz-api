package com.eduquiz.feature.examsession.repository;

import com.eduquiz.feature.auth.entity.User;
import com.eduquiz.feature.exam.entity.Exam;
import com.eduquiz.feature.examroom.entity.ExamRoom;
import com.eduquiz.feature.examsession.entity.ExamSession;
import com.eduquiz.feature.examsession.entity.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExamSessionRepository extends JpaRepository<ExamSession, UUID> {

    List<ExamSession> findByUserOrderByCreatedAtDesc(User user);

    Page<ExamSession> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    Optional<ExamSession> findByUserAndExamAndRoom(User user, Exam exam, ExamRoom room);

    Optional<ExamSession> findByUserAndExamAndRoomIsNull(User user, Exam exam);

    List<ExamSession> findByRoom(ExamRoom room);

    List<ExamSession> findByRoomAndStatus(ExamRoom room, SessionStatus status);
}
