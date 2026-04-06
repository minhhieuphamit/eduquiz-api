package com.eduquiz.feature.examsession.entity;

import com.eduquiz.feature.question.entity.Question;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "exam_answers",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_session_question",
        columnNames = {"session_id", "question_id"}
    )
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamAnswer {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ExamSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    /**
     * Stores selected answer(s).
     * - SINGLE_CHOICE: "A" (single label)
     * - MULTI_CHOICE:  "A,C" (comma-separated sorted labels)
     */
    @Column(name = "answer_content", columnDefinition = "TEXT")
    private String answerContent;

    // Populated after submission/grading
    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
