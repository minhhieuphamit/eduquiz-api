package com.eduquiz.kafka.consumer;

import com.eduquiz.feature.leaderboard.service.LeaderboardService;
import com.eduquiz.kafka.dto.ExamGradedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LeaderboardConsumer {

    private final LeaderboardService leaderboardService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "exam-graded",
            groupId = "eduquiz-leaderboard",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        try {
            ExamGradedEvent event = objectMapper.convertValue(record.value(), ExamGradedEvent.class);
            log.info("[LeaderboardConsumer] Updating leaderboard: sessionId={}, userId={}, score={}",
                    event.getSessionId(), event.getUserId(), event.getScore());
            leaderboardService.updateLeaderboard(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("[LeaderboardConsumer] Failed to process record: {}", e.getMessage(), e);
            ack.acknowledge();
        }
    }
}
