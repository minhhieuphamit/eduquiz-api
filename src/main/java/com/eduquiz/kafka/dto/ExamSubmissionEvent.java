package com.eduquiz.kafka.dto;

/**
 * Event khi student nộp bài.
 * Fields: sessionId, userId, examId, roomId(nullable),
 * answers(List<AnswerItem>: questionId, selectedAnswer), submittedAt
 * TODO: @Data @Builder
 */
public class ExamSubmissionEvent {
}
