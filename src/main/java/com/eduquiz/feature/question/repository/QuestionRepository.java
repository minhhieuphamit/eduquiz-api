package com.eduquiz.feature.question.repository;

import com.eduquiz.feature.question.entity.Difficulty;
import com.eduquiz.feature.question.entity.Question;
import com.eduquiz.feature.question.entity.QuestionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuestionRepository extends JpaRepository<Question, UUID>, JpaSpecificationExecutor<Question> {

    Page<Question> findByChapterIdAndIsActiveTrue(UUID chapterId, Pageable pageable);

    Page<Question> findByChapterIdAndDifficultyAndIsActiveTrue(UUID chapterId, Difficulty difficulty, Pageable pageable);

    Page<Question> findByChapterIdAndTypeAndIsActiveTrue(UUID chapterId, QuestionType type, Pageable pageable);

    Page<Question> findByChapterIdAndDifficultyAndTypeAndIsActiveTrue(UUID chapterId, Difficulty difficulty, QuestionType type, Pageable pageable);

    Optional<Question> findByIdAndIsActiveTrue(UUID id);

    long countByChapterIdAndIsActiveTrue(UUID chapterId);

    @Query("SELECT q FROM Question q WHERE q.chapter.subject.id = :subjectId AND q.isActive = true ORDER BY FUNCTION('RANDOM')")
    List<Question> findRandomBySubjectId(@Param("subjectId") UUID subjectId, Pageable pageable);

    @Query("SELECT q FROM Question q WHERE q.chapter.id IN :chapterIds AND q.difficulty = :difficulty AND q.isActive = true ORDER BY FUNCTION('RANDOM')")
    List<Question> findRandomByChapterIdsAndDifficulty(
            @Param("chapterIds") List<UUID> chapterIds,
            @Param("difficulty") Difficulty difficulty,
            Pageable pageable);
}
