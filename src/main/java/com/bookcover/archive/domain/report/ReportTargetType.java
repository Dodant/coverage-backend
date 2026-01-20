package com.bookcover.archive.domain.report;

/**
 * 신고 대상 유형.
 * 원서, 번역서, 리디자인, 댓글에 대한 신고를 지원합니다.
 */
public enum ReportTargetType {
    /** 원서 */
    ORIGINAL_BOOK,
    /** 번역서 */
    TRANSLATED_BOOK,
    /** 리디자인 */
    REDESIGN,
    /** 댓글 */
    COMMENT
}
