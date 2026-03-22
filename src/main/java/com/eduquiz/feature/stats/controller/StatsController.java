package com.eduquiz.feature.stats.controller;

import com.eduquiz.common.dto.ApiResponse;
import com.eduquiz.feature.stats.dto.AdminStatsResponse;
import com.eduquiz.feature.stats.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Statistics", description = "Thống kê hệ thống")
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/admin/overview")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Thống kê tổng quan hệ thống - ADMIN")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getAdminOverview() {
        log.info("[StatsController.getAdminOverview] START");
        ApiResponse<AdminStatsResponse> response = statsService.getAdminStats();
        log.info("[StatsController.getAdminOverview] SUCCESS - code={}", response.getCode());
        return ResponseEntity.ok(response);
    }
}
