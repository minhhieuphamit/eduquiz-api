package com.eduquiz.feature.examroom.entity;

import com.eduquiz.feature.exam.entity.Exam;
import com.eduquiz.feature.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "room_participants")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomParticipant {

    @EmbeddedId
    private RoomParticipantId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("roomId")
    @JoinColumn(name = "room_id")
    private ExamRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id")
    private Exam exam; // Đề thiHS nhận được

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
    }

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomParticipantId implements Serializable {
        @Column(name = "room_id")
        private UUID roomId;

        @Column(name = "user_id")
        private UUID userId;
    }
}
