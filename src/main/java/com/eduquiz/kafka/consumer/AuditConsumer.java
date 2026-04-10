package com.eduquiz.kafka.consumer;

import com.eduquiz.audit.entity.AuditLog;
import com.eduquiz.audit.repository.AuditLogRepository;
import com.eduquiz.feature.auth.entity.User;
import com.eduquiz.feature.auth.repository.UserRepository;
import com.eduquiz.kafka.dto.AuditEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditConsumer {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "audit-event",
            groupId = "eduquiz-audit",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        try {
            AuditEvent event = objectMapper.convertValue(record.value(), AuditEvent.class);

            User user = null;
            if (event.getUserId() != null) {
                Optional<User> userOpt = userRepository.findById(event.getUserId());
                user = userOpt.orElse(null);
            }

            AuditLog auditLog = AuditLog.builder()
                    .user(user)
                    .action(event.getAction())
                    .entityName(event.getEntityName())
                    .entityId(event.getEntityId())
                    .details(event.getDetail())
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("[AuditConsumer] Saved audit: action={}, userId={}", event.getAction(), event.getUserId());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("[AuditConsumer] Failed to process audit record: {}", e.getMessage(), e);
            ack.acknowledge();
        }
    }
}
