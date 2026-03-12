package com.eduquiz.kafka.producer;

/**
 * Kafka Producer cho exam events.
 * - publishSubmission(ExamSubmissionEvent) → topic "exam-submission"
 * - publishGraded(ExamGradedEvent) → topic "exam-graded"
 * Dùng KafkaTemplate<String, Object>
 * TODO: @Service
 */
public class ExamEventProducer {
}
