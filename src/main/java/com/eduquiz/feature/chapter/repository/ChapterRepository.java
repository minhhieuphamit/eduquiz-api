package com.eduquiz.feature.chapter.repository;

import com.eduquiz.feature.chapter.entity.Chapter;
import com.eduquiz.feature.subject.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, UUID> {
    List<Chapter> findBySubjectOrderByOrderIndexAsc(Subject subject);
}
