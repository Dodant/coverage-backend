package com.bookcover.archive.event;

import java.time.Instant;
import java.util.UUID;

/**
 * 콘텐츠 이벤트 레코드.
 * 원서, 번역서, 리디자인 등록/수정/삭제 이벤트를 나타냅니다.
 *
 * Requirements: 21.1 - 콘텐츠 등록/수정 이벤트
 */
public record ContentEvent(
        UUID eventId,
        ContentEventType eventType,
        UUID entityId,
        UUID userId,
        Instant timestamp
) {
    public ContentEvent(ContentEventType eventType, UUID entityId, UUID userId) {
        this(UUID.randomUUID(), eventType, entityId, userId, Instant.now());
    }
}
