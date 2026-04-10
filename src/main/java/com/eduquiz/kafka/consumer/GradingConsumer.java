package com.eduquiz.kafka.consumer;

import com.eduquiz.kafka.dto.ExamSubmissionEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * GradingConsumer lắng nghe topic "exam-submission".
 *
 * Lưu ý kiến trúc hiện tại: ExamSessionService thực hiện chấm bài đồng bộ (sync)
 * ngay khi student submit, rồi publish ExamGradedEvent trực tiếp.
 * Consumer này dùng để log audit trail và mở rộng sang async grading sau này.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GradingConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "exam-submission",
            groupId = "eduquiz-grading",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        try {
            ExamSubmissionEvent event = objectMapper.convertValue(record.value(), ExamSubmissionEvent.class);
            log.info("[GradingConsumer] Received submission: sessionId={}, userId={}, examId={}",
                    event.getSessionId(), event.getUserId(), event.getExamId());
            // Grading đã được xử lý sync trong ExamSessionService.doSubmit().
            // Consumer này chỉ ghi log — dùng cho audit trail và future async grading.
            ack.acknowledge();
        } catch (Exception e) {
            log.error("[GradingConsumer] Failed to process record: {}", e.getMessage(), e);
            ack.acknowledge(); // ack để tránh requeue vô hạn
        }
    }
}
