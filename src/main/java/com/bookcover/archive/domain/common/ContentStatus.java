package com.bookcover.archive.domain.common;

/**
 * 콘텐츠 상태를 나타내는 열거형.
 * 원서, 번역서, 리디자인 등 모든 콘텐츠에 적용됩니다.
 */
public enum ContentStatus {
    /** 임시 저장 상태 */
    DRAFT,
    /** 공개 상태 */
    PUBLISHED,
    /** 숨김 상태 (신고 등으로 인한 비공개) */
    HIDDEN,
    /** 삭제 상태 */
    DELETED
}
