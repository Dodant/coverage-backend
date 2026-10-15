package com.bookcover.archive.event;

import com.bookcover.archive.config.KafkaConfig;
import com.bookcover.archive.repository.ContentOutboxEventRepository;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Kafka 확인 응답을 받은 후에만 outbox를 발행 완료 처리한다. */
@Component
public class ContentOutboxRelay {
    private static final Logger log = LoggerFactory.getLogger(ContentOutboxRelay.class);
    private final ContentOutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public ContentOutboxRelay(ContentOutboxEventRepository repository, KafkaTemplate<String, String> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${outbox.relay-delay-ms:1000}")
    @Transactional
    public void relayNext() {
        repository.findNextForUpdate(PageRequest.of(0, 1)).stream().findFirst().ifPresent(event -> {
            try {
                kafkaTemplate.send(KafkaConfig.TOPIC_CONTENT_EVENTS,
                        event.getEntityId().toString(), event.getPayload()).get(10, TimeUnit.SECONDS);
                event.setPublishedAt(Instant.now());
            } catch (Exception ex) {
                log.error("콘텐츠 이벤트 발행에 실패했습니다: {}", event.getId(), ex);
            }
        });
    }
}
