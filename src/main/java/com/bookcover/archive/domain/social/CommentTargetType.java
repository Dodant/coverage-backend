package com.bookcover.archive.domain.social;

/**
 * 댓글 대상 유형.
 * 원서 커버, 번역서 커버, 리디자인에 대해 독립적인 댓글을 지원합니다.
 */
public enum CommentTargetType {
    ORIGINAL_COVER,
    TRANSLATED_COVER,
    REDESIGN
}
