package com.eduquiz.kafka.consumer;

/**
 * Kafka Consumer - Chấm bài tự động.
 *
 * @KafkaListener(topics = "exam-submission", groupId = "eduquiz-grading")
 * <p>
 * Flow:
 * 1. Nhận ExamSubmissionEvent
 * 2. Query đáp án đúng từ DB
 * 3. So sánh, tính điểm
 * 4. Update exam_session (score, correctCount, status=GRADED)
 * 5. Update exam_answers (isCorrect)
 * 6. Publish ExamGradedEvent
 * <p>
 * Error handling: @RetryableTopic cho retry, DLT cho dead letter
 * TODO: Implement
 */
public class GradingConsumer {
}
