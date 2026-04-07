package com.eduquiz.feature.examroom.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.eduquiz.feature.examroom.entity.RoomStatus;

@Data
@Builder
public class RoomResultResponse {

    private RoomInfo roomInfo;
    private List<ParticipantResult> participants;

    @Data
    @Builder
    public static class RoomInfo {
        private String roomId;
        private String roomCode;
        private String title;
        private String examTitle;
        private String subjectName;
        private RoomStatus status;
        private int totalQuestions;
        private int totalParticipants;
        private int submittedCount;
    }

    /** One entry per registered participant; contains all their attempts. */
    @Data
    @Builder
    public static class ParticipantResult {
        private String userId;
        private String studentName;
        /** All exam sessions this student had in this room, ordered oldest→newest. */
        private List<AttemptResult> attempts;
    }

    /** One entry per exam session attempt. */
    @Data
    @Builder
    public static class AttemptResult {
        private UUID sessionId;
        private int attemptNumber;   // 1-based
        private String status;       // WAITING / STARTED / SUBMITTED
        private BigDecimal score;
        private Integer correctCount;
        private Integer totalQuestions;
        private LocalDateTime startedAt;
        private LocalDateTime submittedAt;
    }
}
