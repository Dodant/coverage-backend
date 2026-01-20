package com.bookcover.archive.event.listener;

import com.bookcover.archive.config.KafkaConfig;
import com.bookcover.archive.event.ContentEvent;
import com.bookcover.archive.event.ContentEventType;
import com.bookcover.archive.service.SearchService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 검색 인덱스 동기화 리스너.
 * 콘텐츠 이벤트를 수신하여 Elasticsearch 인덱스를 자동으로 업데이트합니다.
 *
 * Requirements: 19.7 - 검색 인덱싱 상태 관리
 */
@Component
public class SearchIndexEventListener {

    private static final Logger log = LoggerFactory.getLogger(SearchIndexEventListener.class);

    private static final Set<ContentEventType> ORIGINAL_BOOK_EVENTS = Set.of(
            ContentEventType.ORIGINAL_BOOK_CREATED,
            ContentEventType.ORIGINAL_BOOK_UPDATED,
            ContentEventType.ORIGINAL_BOOK_DELETED
    );

    private static final Set<ContentEventType> TRANSLATED_BOOK_EVENTS = Set.of(
            ContentEventType.TRANSLATED_BOOK_CREATED,
            ContentEventType.TRANSLATED_BOOK_UPDATED,
            ContentEventType.TRANSLATED_BOOK_DELETED
    );

    private static final Set<ContentEventType> REDESIGN_EVENTS = Set.of(
            ContentEventType.REDESIGN_CREATED,
            ContentEventType.REDESIGN_UPDATED,
            ContentEventType.REDESIGN_DELETED
    );

    private final SearchService searchService;
    private final ObjectMapper objectMapper;

    public SearchIndexEventListener(SearchService searchService, ObjectMapper objectMapper) {
        this.searchService = searchService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = KafkaConfig.TOPIC_CONTENT_EVENTS,
            groupId = "search-index-sync"
    )
    public void handleContentEvent(String message) {
        try {
            ContentEvent event = objectMapper.readValue(message, ContentEvent.class);
            log.debug("Received content event: {} for entity {}", event.eventType(), event.entityId());

            processContentEvent(event);

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize content event: {}", message, e);
        } catch (Exception e) {
            log.error("Failed to process content event: {}", message, e);
        }
    }

    private void processContentEvent(ContentEvent event) {
        ContentEventType eventType = event.eventType();

        if (ORIGINAL_BOOK_EVENTS.contains(eventType)) {
            handleOriginalBookEvent(event);
        } else if (TRANSLATED_BOOK_EVENTS.contains(eventType)) {
            handleTranslatedBookEvent(event);
        } else if (REDESIGN_EVENTS.contains(eventType)) {
            handleRedesignEvent(event);
        } else {
            log.warn("Unknown content event type: {}", eventType);
        }
    }

    private void handleOriginalBookEvent(ContentEvent event) {
        switch (event.eventType()) {
            case ORIGINAL_BOOK_CREATED, ORIGINAL_BOOK_UPDATED -> {
                searchService.indexOriginalBook(event.entityId());
                log.info("Indexed original book: {}", event.entityId());
            }
            case ORIGINAL_BOOK_DELETED -> {
                searchService.removeFromIndex("original_" + event.entityId());
                log.info("Removed original book from index: {}", event.entityId());
            }
            default -> log.warn("Unexpected original book event type: {}", event.eventType());
        }
    }

    private void handleTranslatedBookEvent(ContentEvent event) {
        switch (event.eventType()) {
            case TRANSLATED_BOOK_CREATED, TRANSLATED_BOOK_UPDATED -> {
                searchService.indexTranslatedBook(event.entityId());
                log.info("Indexed translated book: {}", event.entityId());
            }
            case TRANSLATED_BOOK_DELETED -> {
                searchService.removeFromIndex("translated_" + event.entityId());
                log.info("Removed translated book from index: {}", event.entityId());
            }
            default -> log.warn("Unexpected translated book event type: {}", event.eventType());
        }
    }

    private void handleRedesignEvent(ContentEvent event) {
        switch (event.eventType()) {
            case REDESIGN_CREATED, REDESIGN_UPDATED -> {
                searchService.indexRedesign(event.entityId());
                log.info("Indexed redesign: {}", event.entityId());
            }
            case REDESIGN_DELETED -> {
                searchService.removeFromIndex("redesign_" + event.entityId());
                log.info("Removed redesign from index: {}", event.entityId());
            }
            default -> log.warn("Unexpected redesign event type: {}", event.eventType());
        }
    }
}
