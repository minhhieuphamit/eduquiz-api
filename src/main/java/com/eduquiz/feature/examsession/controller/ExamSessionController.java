package com.eduquiz.feature.examsession.controller;

/**
 * @RestController /api/v1/exam-sessions
 * @PreAuthorize("hasRole('STUDENT')") POST /start          → startExam
 * PUT  /{id}/answer    → saveAnswer
 * POST /{id}/submit    → submitExam → Kafka
 * GET  /{id}/result    → getResult
 * GET  /history        → getHistory (paginated)
 * TODO: Implement
 */
public class ExamSessionController {
}
