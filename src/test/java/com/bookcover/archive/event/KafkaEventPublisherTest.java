package com.bookcover.archive.event;

import com.bookcover.archive.config.KafkaConfig;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class KafkaEventPublisherTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> template = mock(KafkaTemplate.class);
    private final KafkaEventPublisher publisher =
            new KafkaEventPublisher(template, new ObjectMapper().registerModule(new JavaTimeModule()));
    private final ContentEvent event =
            new ContentEvent(ContentEventType.ORIGINAL_BOOK_UPDATED, UUID.randomUUID(), null);

    @AfterEach
    void clear() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void 트랜잭션_안에서는_커밋_후에_발행한다() {
        when(template.send(anyString(), anyString(), anyString())).thenReturn(new CompletableFuture<>());
        TransactionSynchronizationManager.initSynchronization();

        publisher.publishContentEvent(event);
        verifyNoInteractions(template);

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(template).send(eq(KafkaConfig.TOPIC_CONTENT_EVENTS), eq(event.entityId().toString()), anyString());
    }

    @Test
    void 롤백되면_발행하지_않는다() {
        TransactionSynchronizationManager.initSynchronization();

        publisher.publishContentEvent(event);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verifyNoInteractions(template);
    }

    @Test
    void 트랜잭션_밖에서는_즉시_발행한다() {
        when(template.send(anyString(), anyString(), anyString())).thenReturn(new CompletableFuture<>());

        publisher.publishContentEvent(event);

        verify(template).send(eq(KafkaConfig.TOPIC_CONTENT_EVENTS), eq(event.entityId().toString()), anyString());
    }
}
