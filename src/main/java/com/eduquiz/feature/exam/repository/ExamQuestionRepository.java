package com.eduquiz.feature.exam.repository;

import com.eduquiz.feature.exam.entity.Exam;
import com.eduquiz.feature.exam.entity.ExamQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, ExamQuestion.ExamQuestionId> {
    List<ExamQuestion> findByExam(Exam exam);

    int countByExam(Exam exam);
}
