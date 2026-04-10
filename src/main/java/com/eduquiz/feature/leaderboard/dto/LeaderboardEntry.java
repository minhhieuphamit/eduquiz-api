package com.eduquiz.feature.leaderboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntry {

    private int rank;
    private UUID userId;
    private String fullName;
    private String subjectName;
    private BigDecimal bestScore;
    private long totalExams;
}
