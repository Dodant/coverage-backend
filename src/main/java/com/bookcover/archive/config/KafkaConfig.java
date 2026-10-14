package com.bookcover.archive.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

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

    /**
     * 리스너 예외 시 1초 간격 3회 재시도 후 {@code <토픽>.DLT}로 보낸다.
     * 역직렬화 실패는 재시도해도 같으므로 바로 DLT로 보낸다.
     * Spring Boot가 이 빈을 기본 리스너 컨테이너 팩토리에 적용한다.
     */
    // ponytail: DLT는 쌓이기만 한다. 재처리는 수동(또는 POST /admin/search/reindex). 자주 쌓이면 DLT 재발행 관리자 API를 추가
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DefaultErrorHandler handler = new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(kafkaTemplate), new FixedBackOff(1000L, 3));
        handler.addNotRetryableExceptions(JsonProcessingException.class);
        return handler;
    }

    @Bean
    public NewTopic contentEventsDltTopic() {
        // DeadLetterPublishingRecoverer는 원본과 같은 파티션 번호로 보내므로 파티션 수를 맞춘다
        return TopicBuilder.name(TOPIC_CONTENT_EVENTS + ".DLT")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
