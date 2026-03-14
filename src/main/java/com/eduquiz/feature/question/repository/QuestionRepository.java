package com.eduquiz.feature.question.repository;

import com.eduquiz.feature.chapter.entity.Chapter;
import com.eduquiz.feature.question.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface QuestionRepository extends JpaRepository<Question, UUID> {
    Page<Question> findByChapter(Chapter chapter, Pageable pageable);
}
