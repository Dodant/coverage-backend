package com.bookcover.archive.event;

import com.bookcover.archive.domain.report.ReportTargetType;

import java.time.Instant;
import java.util.UUID;

/**
 * 모더레이션 이벤트 레코드.
 * 신고, 자동 비공개, 콘텐츠 관리 이벤트를 나타냅니다.
 *
 * Requirements: 15.4 - 신고 자동 비공개 알림
 */
public record ModerationEvent(
        UUID eventId,
        ModerationEventType eventType,
        ReportTargetType targetType,
        UUID targetId,
        UUID actorId,
        String reason,
        Instant timestamp
) {
    public ModerationEvent(ModerationEventType eventType, ReportTargetType targetType,
                           UUID targetId, UUID actorId, String reason) {
        this(UUID.randomUUID(), eventType, targetType, targetId, actorId, reason, Instant.now());
    }
}
