package com.bookcover.archive.event.listener;

import com.bookcover.archive.config.KafkaConfig;
import com.bookcover.archive.event.ModerationEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 모더레이션 이벤트 리스너.
 * 신고 자동 비공개 등 모더레이션 이벤트를 수신하여 알림을 처리합니다.
 *
 * Requirements: 15.4 - 신고 자동 비공개 알림
 */
@Component
public class ModerationEventListener {

    private static final Logger log = LoggerFactory.getLogger(ModerationEventListener.class);

    private final ObjectMapper objectMapper;

    public ModerationEventListener(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = KafkaConfig.TOPIC_MODERATION_EVENTS,
            groupId = "moderation-notification"
    )
    public void handleModerationEvent(String message) {
        try {
            ModerationEvent event = objectMapper.readValue(message, ModerationEvent.class);
            log.debug("Received moderation event: {} for target {}/{}",
                    event.eventType(), event.targetType(), event.targetId());

            processModerationEvent(event);

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize moderation event: {}", message, e);
        } catch (Exception e) {
            log.error("Failed to process moderation event: {}", message, e);
        }
    }

    private void processModerationEvent(ModerationEvent event) {
        switch (event.eventType()) {
            case CONTENT_AUTO_HIDDEN -> handleAutoHiddenEvent(event);
            case CONTENT_REPORTED -> handleReportedEvent(event);
            case CONTENT_HIDDEN -> handleHiddenEvent(event);
            case CONTENT_RESTORED -> handleRestoredEvent(event);
            case CONTENT_DELETED -> handleDeletedEvent(event);
            case USER_SUSPENDED -> handleUserSuspendedEvent(event);
            case USER_REPORT_RESTRICTED -> handleUserReportRestrictedEvent(event);
            default -> log.warn("Unknown moderation event type: {}", event.eventType());
        }
    }

    private void handleAutoHiddenEvent(ModerationEvent event) {
        // 자동 비공개 처리 알림 - 관리자에게 알림
        log.info("Content auto-hidden: type={}, id={}, reason={}",
                event.targetType(), event.targetId(), event.reason());

        // TODO: 실제 알림 시스템 연동 (이메일, 푸시 알림 등)
        // 예: notificationService.notifyAdmins("Content auto-hidden", event);
    }

    private void handleReportedEvent(ModerationEvent event) {
        // 신고 접수 알림
        log.info("Content reported: type={}, id={}, reporter={}",
                event.targetType(), event.targetId(), event.actorId());

        // TODO: 신고 큐에 추가 및 관리자 알림
    }

    private void handleHiddenEvent(ModerationEvent event) {
        // 콘텐츠 숨김 처리 알림 - 콘텐츠 소유자에게 알림
        log.info("Content hidden by admin: type={}, id={}, reason={}",
                event.targetType(), event.targetId(), event.reason());

        // TODO: 콘텐츠 소유자에게 알림
    }

    private void handleRestoredEvent(ModerationEvent event) {
        // 콘텐츠 복원 알림
        log.info("Content restored: type={}, id={}",
                event.targetType(), event.targetId());

        // TODO: 콘텐츠 소유자에게 복원 알림
    }

    private void handleDeletedEvent(ModerationEvent event) {
        // 콘텐츠 삭제 알림 - 콘텐츠 소유자에게 사유 통지
        log.info("Content deleted: type={}, id={}, reason={}",
                event.targetType(), event.targetId(), event.reason());

        // TODO: 콘텐츠 소유자에게 삭제 사유 통지
    }

    private void handleUserSuspendedEvent(ModerationEvent event) {
        // 사용자 정지 알림
        log.info("User suspended: userId={}, reason={}",
                event.targetId(), event.reason());

        // TODO: 정지된 사용자에게 알림
    }

    private void handleUserReportRestrictedEvent(ModerationEvent event) {
        // 신고 기능 제한 알림
        log.info("User report restricted: userId={}, reason={}",
                event.targetId(), event.reason());

        // TODO: 사용자에게 신고 기능 제한 알림
    }
}
