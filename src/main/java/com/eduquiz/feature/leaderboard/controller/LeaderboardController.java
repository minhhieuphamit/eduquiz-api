package com.eduquiz.feature.leaderboard.controller;

import com.eduquiz.common.dto.ApiResponse;
import com.eduquiz.feature.leaderboard.dto.LeaderboardEntry;
import com.eduquiz.feature.leaderboard.service.LeaderboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leaderboard")
@RequiredArgsConstructor
@Tag(name = "Leaderboard", description = "Xếp hạng theo môn học")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    /**
     * GET /api/v1/leaderboard?subjectId=...&limit=50
     * Lấy bảng xếp hạng theo môn. subjectId = null → tất cả môn.
     */
    @GetMapping
    @Operation(summary = "Lấy leaderboard", description = "Xếp hạng theo best score. subjectId optional.")
    public ApiResponse<List<LeaderboardEntry>> getLeaderboard(
            @RequestParam(required = false) UUID subjectId,
            @RequestParam(defaultValue = "50") int limit) {

        List<LeaderboardEntry> entries = leaderboardService.getLeaderboard(subjectId, Math.min(limit, 100));
        return ApiResponse.ok(entries);
    }
}
