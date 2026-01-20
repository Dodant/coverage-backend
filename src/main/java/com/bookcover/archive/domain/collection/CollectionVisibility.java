package com.bookcover.archive.domain.collection;

/**
 * 컬렉션 공개 설정.
 * 비공개, 공개, 링크 공유(읽기 전용)를 지원합니다.
 */
public enum CollectionVisibility {
    PRIVATE,
    PUBLIC,
    LINK_ONLY
}
