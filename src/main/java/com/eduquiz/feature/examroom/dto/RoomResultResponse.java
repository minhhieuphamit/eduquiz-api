package com.eduquiz.feature.examroom.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

    @Data
    @Builder
    public static class ParticipantResult {
        private String userId;
        private String studentName;
        private String status;
        private BigDecimal score;
        private Integer correctCount;
        private Integer totalQuestions;
        private LocalDateTime submittedAt;
    }
}
