package com.eduquiz.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic examSubmissionTopic() {
        return TopicBuilder.name("exam-submission")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic examGradedTopic() {
        return TopicBuilder.name("exam-graded")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic auditEventTopic() {
        return TopicBuilder.name("audit-event")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
