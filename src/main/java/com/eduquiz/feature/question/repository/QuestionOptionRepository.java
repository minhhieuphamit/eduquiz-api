package com.eduquiz.feature.question.repository;

import com.eduquiz.feature.question.entity.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuestionOptionRepository extends JpaRepository<QuestionOption, UUID> {

    List<QuestionOption> findByQuestionIdOrderByOrderIndexAsc(UUID questionId);

    void deleteByQuestionId(UUID questionId);
}
