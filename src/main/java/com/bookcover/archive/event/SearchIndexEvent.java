package com.bookcover.archive.event;

import java.time.Instant;
import java.util.UUID;

/**
 * 검색 인덱스 이벤트 레코드.
 * Elasticsearch 인덱스 동기화를 위한 이벤트입니다.
 *
 * Requirements: 19.7 - 검색 인덱싱 상태 관리
 */
public record SearchIndexEvent(
        UUID eventId,
        SearchIndexAction action,
        String documentType,
        UUID entityId,
        Instant timestamp
) {
    public SearchIndexEvent(SearchIndexAction action, String documentType, UUID entityId) {
        this(UUID.randomUUID(), action, documentType, entityId, Instant.now());
    }

    public enum SearchIndexAction {
        INDEX,
        UPDATE,
        DELETE
    }
}
