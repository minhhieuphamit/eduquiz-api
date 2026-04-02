package com.eduquiz.feature.examroom.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomResultResponse {

    private RoomInfo roomInfo;
    private List<ParticipantResult> participants;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomInfo {
        private UUID roomId;
        private String roomCode;
        private String title;
        private String examTitle;
        private String subjectName;
        private int totalQuestions;
        private int totalParticipants;
        private int submittedCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantResult {
        private UUID userId;
        private String studentName;
        private BigDecimal score;
        private Integer correctCount;
        private Integer totalQuestions;
        private LocalDateTime submittedAt;
        private String status;
    }
}
