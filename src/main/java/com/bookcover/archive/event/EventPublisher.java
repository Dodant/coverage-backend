package com.bookcover.archive.event;

/**
 * 이벤트 발행 인터페이스.
 * Kafka를 통해 비동기 이벤트를 발행합니다.
 *
 * Requirements: 21.1 - 콘텐츠 등록/수정 이벤트
 */
public interface EventPublisher {

    /**
     * 콘텐츠 이벤트를 발행합니다.
     *
     * @param event 콘텐츠 이벤트
     */
    void publishContentEvent(ContentEvent event);

    /**
     * 모더레이션 이벤트를 발행합니다.
     *
     * @param event 모더레이션 이벤트
     */
    void publishModerationEvent(ModerationEvent event);

    /**
     * 검색 인덱스 이벤트를 발행합니다.
     *
     * @param event 검색 인덱스 이벤트
     */
    void publishSearchIndexEvent(SearchIndexEvent event);
}
