package com.bookcover.archive.event;

/**
 * 모더레이션 이벤트 유형.
 */
public enum ModerationEventType {
    CONTENT_REPORTED,
    CONTENT_AUTO_HIDDEN,
    CONTENT_HIDDEN,
    CONTENT_RESTORED,
    CONTENT_DELETED,
    USER_SUSPENDED,
    USER_REPORT_RESTRICTED
}
