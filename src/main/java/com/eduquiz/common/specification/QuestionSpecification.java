package com.eduquiz.common.specification;

import com.eduquiz.feature.question.entity.Difficulty;
import com.eduquiz.feature.question.entity.Question;
import com.eduquiz.feature.question.entity.QuestionType;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class QuestionSpecification {

    private QuestionSpecification() {}

    public static Specification<Question> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("isActive"));
    }

    public static Specification<Question> hasChapterId(UUID chapterId) {
        return (root, query, cb) -> {
            if (chapterId == null) return cb.conjunction();
            return cb.equal(root.get("chapter").get("id"), chapterId);
        };
    }

    public static Specification<Question> hasDifficulty(Difficulty difficulty) {
        return (root, query, cb) -> {
            if (difficulty == null) return cb.conjunction();
            return cb.equal(root.get("difficulty"), difficulty);
        };
    }

    public static Specification<Question> hasType(QuestionType type) {
        return (root, query, cb) -> {
            if (type == null) return cb.conjunction();
            return cb.equal(root.get("type"), type);
        };
    }

    public static Specification<Question> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            return cb.like(cb.lower(root.get("content")), pattern);
        };
    }

    public static Specification<Question> hasSubjectId(UUID subjectId) {
        return (root, query, cb) -> {
            if (subjectId == null) return cb.conjunction();
            return cb.equal(root.get("chapter").get("subject").get("id"), subjectId);
        };
    }

    /**
     * Teacher visibility: câu hỏi do mình tạo HOẶC câu hỏi được share (isShared = true).
     */
    public static Specification<Question> visibleToTeacher(UUID teacherId) {
        return (root, query, cb) -> cb.or(
                cb.equal(root.get("createdBy").get("id"), teacherId),
                cb.isTrue(root.get("isShared"))
        );
    }
}
