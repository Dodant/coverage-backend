package com.bookcover.archive.domain.social;

/**
 * 좋아요 대상 유형.
 * 원서 커버, 번역서 커버, 리디자인에 대해 독립적인 좋아요를 지원합니다.
 */
public enum LikeTargetType {
    ORIGINAL_COVER,
    TRANSLATED_COVER,
    REDESIGN
}
