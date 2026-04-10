package com.eduquiz.kafka.producer;

import com.eduquiz.kafka.dto.AuditEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditEventProducer {

    private static final String TOPIC = "audit-event";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async
    public void publish(AuditEvent event) {
        String key = event.getUserId() != null ? event.getUserId().toString() : "anonymous";
        kafkaTemplate.send(TOPIC, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka] Failed to publish AuditEvent action={}: {}",
                                event.getAction(), ex.getMessage());
                    } else {
                        log.debug("[Kafka] Published AuditEvent action={}, userId={}",
                                event.getAction(), event.getUserId());
                    }
                });
    }

    // ── Convenience builder methods ──

    public void log(UUID userId, String action, String entityName, UUID entityId, Map<String, Object> detail) {
        publish(AuditEvent.builder()
                .userId(userId)
                .action(action)
                .entityName(entityName)
                .entityId(entityId)
                .detail(detail)
                .timestamp(LocalDateTime.now())
                .build());
    }

    public void log(UUID userId, String action, Map<String, Object> detail) {
        log(userId, action, null, null, detail);
    }
}
