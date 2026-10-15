package com.bookcover.archive.event;

import com.bookcover.archive.repository.ContentOutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class KafkaEventPublisherTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> template = mock(KafkaTemplate.class);
    private final ContentOutboxEventRepository outboxRepository = mock(ContentOutboxEventRepository.class);
    private final KafkaEventPublisher publisher =
            new KafkaEventPublisher(template, new ObjectMapper().registerModule(new JavaTimeModule()), outboxRepository);
    private final ContentEvent event =
            new ContentEvent(ContentEventType.ORIGINAL_BOOK_UPDATED, UUID.randomUUID(), null);

    @AfterEach
    void clear() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void 트랜잭션_안에서는_outbox에_저장한다() {
        TransactionSynchronizationManager.initSynchronization();

        publisher.publishContentEvent(event);
        verifyNoInteractions(template);
        verify(outboxRepository).save(org.mockito.ArgumentMatchers.any(ContentOutboxEvent.class));
    }

    @Test
    void 롤백시에도_Kafka로_직접_발행하지_않는다() {
        TransactionSynchronizationManager.initSynchronization();

        publisher.publishContentEvent(event);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verifyNoInteractions(template);
    }

    @Test
    void 트랜잭션_밖에서도_outbox에_저장한다() {
        publisher.publishContentEvent(event);

        verify(outboxRepository).save(org.mockito.ArgumentMatchers.any(ContentOutboxEvent.class));
        verifyNoInteractions(template);
    }

    @Test
    void 두_이벤트를_모두_outbox에_저장한다() {
        TransactionSynchronizationManager.initSynchronization();

        publisher.publishContentEvent(event);
        publisher.publishContentEvent(new ContentEvent(ContentEventType.REDESIGN_UPDATED, UUID.randomUUID(), null));

        verify(outboxRepository, times(2)).save(org.mockito.ArgumentMatchers.any(ContentOutboxEvent.class));
        verifyNoInteractions(template);
    }
}
