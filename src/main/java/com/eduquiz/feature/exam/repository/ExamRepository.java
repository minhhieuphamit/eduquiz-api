package com.eduquiz.feature.exam.repository;

import com.eduquiz.feature.exam.entity.Exam;
import com.eduquiz.feature.exam.entity.ExamType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExamRepository extends JpaRepository<Exam, UUID> {

    Page<Exam> findBySubjectIdAndIsActiveTrueOrderByYearDescCreatedAtDesc(UUID subjectId, Pageable pageable);

    Page<Exam> findBySubjectIdAndExamTypeAndIsActiveTrueOrderByYearDescCreatedAtDesc(UUID subjectId, ExamType examType, Pageable pageable);

    Page<Exam> findBySubjectIdAndYearAndIsActiveTrueOrderByCreatedAtDesc(UUID subjectId, Integer year, Pageable pageable);

    Page<Exam> findByIsActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    Optional<Exam> findByIdAndIsActiveTrue(UUID id);

    List<Exam> findDistinctYearBySubjectIdAndIsActiveTrue(UUID subjectId);
}
