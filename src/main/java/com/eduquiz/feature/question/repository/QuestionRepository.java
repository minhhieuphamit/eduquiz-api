package com.eduquiz.feature.question.repository;

/**
 * JpaRepository<Question, Long>
 * - findByChapterId(chapterId, Pageable)
 * - findByChapterIdAndDifficulty(chapterId, difficulty, Pageable)
 * - findByChapter_Subject_Id(subjectId) → dùng cho random câu hỏi
 * - findRandomBySubjectAndDifficulty(subjectId, difficulty, limit) → @Query native random
 * TODO: Implement
 */
public interface QuestionRepository {
}
