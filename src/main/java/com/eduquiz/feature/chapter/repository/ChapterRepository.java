package com.eduquiz.feature.chapter.repository;

import com.eduquiz.feature.chapter.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, UUID> {

    List<Chapter> findBySubjectIdAndIsActiveTrueOrderByOrderIndexAsc(UUID subjectId);

    Optional<Chapter> findByIdAndIsActiveTrue(UUID id);

    boolean existsByNameAndSubjectIdAndIsActiveTrue(String name, UUID subjectId);

    boolean existsByNameAndSubjectIdAndIdNotAndIsActiveTrue(String name, UUID subjectId, UUID id);

    @Query("SELECT COALESCE(MAX(c.orderIndex), 0) FROM Chapter c WHERE c.subject.id = :subjectId AND c.isActive = true")
    int findMaxOrderIndexBySubjectId(@Param("subjectId") UUID subjectId);

    long countBySubjectIdAndIsActiveTrue(UUID subjectId);
}
