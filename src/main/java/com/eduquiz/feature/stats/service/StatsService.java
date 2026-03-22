package com.eduquiz.feature.stats.service;

import com.eduquiz.common.constant.ResponseCode;
import com.eduquiz.common.dto.ApiResponse;
import com.eduquiz.feature.auth.repository.UserRepository;
import com.eduquiz.feature.exam.repository.ExamRepository;
import com.eduquiz.feature.question.repository.QuestionRepository;
import com.eduquiz.feature.stats.dto.AdminStatsResponse;
import com.eduquiz.feature.subject.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsService {

    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final QuestionRepository questionRepository;
    private final ExamRepository examRepository;

    public ApiResponse<AdminStatsResponse> getAdminStats() {
        log.info("[StatsService.getAdminStats] START");

        AdminStatsResponse stats = AdminStatsResponse.builder()
                .totalUsers(userRepository.count())
                .totalSubjects(subjectRepository.count())
                .totalQuestions(questionRepository.count())
                .totalExams(examRepository.count())
                .build();

        log.info("[StatsService.getAdminStats] SUCCESS - users={}, subjects={}, questions={}, exams={}",
                stats.getTotalUsers(), stats.getTotalSubjects(),
                stats.getTotalQuestions(), stats.getTotalExams());

        return ApiResponse.ok(ResponseCode.STATS_ADMIN_OVERVIEW_SUCCESS, stats);
    }
}
