package com.eduquiz.kafka.config;

/**
 * Kafka Producer + Consumer configuration.
 * Topics: exam-submission, exam-graded, audit-event
 * Serializer: JsonSerializer/JsonDeserializer
 * Consumer group: eduquiz-grading, eduquiz-leaderboard, eduquiz-audit
 * TODO: @Configuration + @Bean ProducerFactory, ConsumerFactory, KafkaTemplate
 */
public class KafkaConfig {
}
