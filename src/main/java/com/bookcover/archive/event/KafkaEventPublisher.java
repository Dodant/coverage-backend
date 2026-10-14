package com.bookcover.archive.event;

import com.bookcover.archive.config.KafkaConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka 기반 이벤트 발행자 구현체.
 * 콘텐츠, 모더레이션, 검색 인덱스 이벤트를 Kafka 토픽으로 발행합니다.
 *
 * Requirements: 21.1 - 콘텐츠 등록/수정 이벤트
 */
@Component
public class KafkaEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishContentEvent(ContentEvent event) {
        String key = event.entityId().toString();
        String payload = serialize(event);

        if (payload != null) {
            sendMessage(KafkaConfig.TOPIC_CONTENT_EVENTS, key, payload, "ContentEvent");
        }
    }

    @Override
    public void publishModerationEvent(ModerationEvent event) {
        String key = event.targetId().toString();
        String payload = serialize(event);

        if (payload != null) {
            sendMessage(KafkaConfig.TOPIC_MODERATION_EVENTS, key, payload, "ModerationEvent");
        }
    }

    @Override
    public void publishSearchIndexEvent(SearchIndexEvent event) {
        String key = event.entityId().toString();
        String payload = serialize(event);

        if (payload != null) {
            sendMessage(KafkaConfig.TOPIC_SEARCH_INDEX_EVENTS, key, payload, "SearchIndexEvent");
        }
    }

    private String serialize(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event: {}", event, e);
            return null;
        }
    }

    /**
     * 트랜잭션 안에서 호출되면 커밋 후에 보낸다. 컨슈머가 커밋 전 DB를 읽어 옛 값으로 색인하거나,
     * 롤백된 변경이 이벤트로 나가는 것을 막는다. 트랜잭션 밖이면 즉시 보낸다.
     */
    private void sendMessage(String topic, String key, String payload, String eventType) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(topic, key, payload, eventType);
                }
            });
        } else {
            send(topic, key, payload, eventType);
        }
    }

    // ponytail: 커밋과 발행 사이에 앱이 죽으면 이벤트가 유실된다. 유실이 문제되면 outbox 테이블로
    private void send(String topic, String key, String payload, String eventType) {
        CompletableFuture<SendResult<String, String>> future;
        try {
            future = kafkaTemplate.send(topic, key, payload);
        } catch (Exception ex) {
            // afterCommit에서 던지면 이미 커밋된 요청이 500이 되고 뒤에 등록된 이벤트도 건너뛴다. 로그만 남긴다
            log.error("Failed to send {} to topic {} (key {})", eventType, topic, key, ex);
            return;
        }

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send {} to topic {}: {}", eventType, topic, ex.getMessage());
            } else {
                log.debug("Sent {} to topic {} with offset {}",
                        eventType, topic, result.getRecordMetadata().offset());
            }
        });
    }
}
