package com.bookcover.archive.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka 설정 클래스.
 * 토픽 생성 및 Kafka 관련 설정을 담당합니다.
 *
 * Requirements: 21.1 - 확장 가능한 구조로 설계
 */
@Configuration
public class KafkaConfig {

    public static final String TOPIC_CONTENT_EVENTS = "bookcover.content.events";
    public static final String TOPIC_MODERATION_EVENTS = "bookcover.moderation.events";
    public static final String TOPIC_SEARCH_INDEX_EVENTS = "bookcover.search.index";

    @Bean
    public NewTopic contentEventsTopic() {
        return TopicBuilder.name(TOPIC_CONTENT_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic moderationEventsTopic() {
        return TopicBuilder.name(TOPIC_MODERATION_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic searchIndexEventsTopic() {
        return TopicBuilder.name(TOPIC_SEARCH_INDEX_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
