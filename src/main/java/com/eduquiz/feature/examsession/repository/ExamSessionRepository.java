package com.eduquiz.feature.examsession.repository;

import com.eduquiz.feature.examsession.entity.ExamSession;
import com.eduquiz.feature.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExamSessionRepository extends JpaRepository<ExamSession, UUID> {
    List<ExamSession> findByUserOrderByCreatedAtDesc(User user);
}
