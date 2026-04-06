package com.eduquiz.feature.examsession.repository;

import com.eduquiz.feature.examsession.entity.ExamAnswer;
import com.eduquiz.feature.examsession.entity.ExamSession;
import com.eduquiz.feature.question.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExamAnswerRepository extends JpaRepository<ExamAnswer, UUID> {

    List<ExamAnswer> findBySession(ExamSession session);

    Optional<ExamAnswer> findBySessionAndQuestion(ExamSession session, Question question);

    long countBySessionAndIsCorrectTrue(ExamSession session);

    // Delete all answers for a session (used in tests / reset)
    @Modifying
    @Query("DELETE FROM ExamAnswer a WHERE a.session = :session")
    void deleteBySession(@Param("session") ExamSession session);
}
