package com.eduquiz.feature.examsession.service;

/**
 * Exam Session Service.
 * - startExam(request, userId): tạo session + exam_answers (blank)
 * - saveAnswer(sessionId, answerRequest, userId): lưu đáp án tạm
 * - submitExam(sessionId, userId): set SUBMITTED, publish Kafka ExamSubmissionEvent
 * - getResult(sessionId, userId): xem kết quả (sau khi GRADED)
 * - getHistory(userId, Pageable): lịch sử làm bài
 * TODO: @Service
 */
public class ExamSessionService {
}
