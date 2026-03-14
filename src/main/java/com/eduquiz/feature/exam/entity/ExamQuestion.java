package com.eduquiz.feature.exam.entity;

import com.eduquiz.feature.question.entity.Question;
import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "exam_questions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamQuestion {

    @EmbeddedId
    private ExamQuestionId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("examId")
    @JoinColumn(name = "exam_id")
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("questionId")
    @JoinColumn(name = "question_id")
    private Question question;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExamQuestionId implements Serializable {
        @Column(name = "exam_id")
        private UUID examId;

        @Column(name = "question_id")
        private UUID questionId;
    }
}
