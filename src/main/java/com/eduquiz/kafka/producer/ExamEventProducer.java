package com.eduquiz.kafka.producer;

import com.eduquiz.kafka.dto.ExamGradedEvent;
import com.eduquiz.kafka.dto.ExamSubmissionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExamEventProducer {

    private static final String TOPIC_SUBMISSION = "exam-submission";
    private static final String TOPIC_GRADED     = "exam-graded";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishSubmission(ExamSubmissionEvent event) {
        String key = event.getSessionId().toString();
        kafkaTemplate.send(TOPIC_SUBMISSION, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka] Failed to publish ExamSubmissionEvent sessionId={}: {}",
                                event.getSessionId(), ex.getMessage());
                    } else {
                        log.debug("[Kafka] Published ExamSubmissionEvent sessionId={} → partition={}",
                                event.getSessionId(),
                                result.getRecordMetadata().partition());
                    }
                });
    }

    public void publishGraded(ExamGradedEvent event) {
        String key = event.getSessionId().toString();
        kafkaTemplate.send(TOPIC_GRADED, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka] Failed to publish ExamGradedEvent sessionId={}: {}",
                                event.getSessionId(), ex.getMessage());
                    } else {
                        log.info("[Kafka] Published ExamGradedEvent sessionId={}, score={} → partition={}",
                                event.getSessionId(), event.getScore(),
                                result.getRecordMetadata().partition());
                    }
                });
    }
}
