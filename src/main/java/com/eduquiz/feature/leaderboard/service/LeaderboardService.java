package com.eduquiz.feature.leaderboard.service;

import com.eduquiz.feature.leaderboard.dto.LeaderboardEntry;
import com.eduquiz.kafka.dto.ExamGradedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaderboardService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Lấy leaderboard theo môn học.
     * Xếp hạng theo best score, tiebreak bằng số lần làm bài.
     *
     * @param subjectId UUID của môn học (null = tất cả môn)
     * @param limit     số entry tối đa (default 50)
     */
    public List<LeaderboardEntry> getLeaderboard(UUID subjectId, int limit) {
        String sql;
        Object[] params;

        if (subjectId != null) {
            sql = """
                SELECT
                    u.id           AS user_id,
                    u.first_name   AS first_name,
                    u.last_name    AS last_name,
                    s.name         AS subject_name,
                    MAX(es.score)  AS best_score,
                    COUNT(es.id)   AS total_exams
                FROM exam_sessions es
                JOIN users  u ON u.id = es.user_id
                JOIN exams  e ON e.id = es.exam_id
                JOIN subjects s ON s.id = e.subject_id
                WHERE es.status IN ('SUBMITTED', 'AUTO_SUBMITTED')
                  AND e.subject_id = ?::uuid
                GROUP BY u.id, u.first_name, u.last_name, s.name
                ORDER BY best_score DESC, total_exams DESC
                LIMIT ?
                """;
            params = new Object[]{subjectId.toString(), limit};
        } else {
            sql = """
                SELECT
                    u.id           AS user_id,
                    u.first_name   AS first_name,
                    u.last_name    AS last_name,
                    s.name         AS subject_name,
                    MAX(es.score)  AS best_score,
                    COUNT(es.id)   AS total_exams
                FROM exam_sessions es
                JOIN users  u ON u.id = es.user_id
                JOIN exams  e ON e.id = es.exam_id
                JOIN subjects s ON s.id = e.subject_id
                WHERE es.status IN ('SUBMITTED', 'AUTO_SUBMITTED')
                GROUP BY u.id, u.first_name, u.last_name, s.name
                ORDER BY best_score DESC, total_exams DESC
                LIMIT ?
                """;
            params = new Object[]{limit};
        }

        List<LeaderboardEntry> results = new ArrayList<>();
        int[] rankHolder = {1};

        jdbcTemplate.query(sql, params, rs -> {
            UUID userId = UUID.fromString(rs.getString("user_id"));
            String fullName = rs.getString("first_name") + " " + rs.getString("last_name");
            String subjectName = rs.getString("subject_name");
            BigDecimal bestScore = rs.getBigDecimal("best_score");
            long totalExams = rs.getLong("total_exams");

            results.add(LeaderboardEntry.builder()
                    .rank(rankHolder[0]++)
                    .userId(userId)
                    .fullName(fullName.trim())
                    .subjectName(subjectName)
                    .bestScore(bestScore)
                    .totalExams(totalExams)
                    .build());
        });

        return results;
    }

    /**
     * Được gọi bởi LeaderboardConsumer khi nhận ExamGradedEvent.
     * Leaderboard tính on-demand từ exam_sessions, method này log event flow.
     */
    public void updateLeaderboard(ExamGradedEvent event) {
        log.info("[Leaderboard] Received graded event: userId={}, subjectId={}, score={}",
                event.getUserId(), event.getSubjectId(), event.getScore());
    }
}
