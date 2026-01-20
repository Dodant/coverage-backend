package com.bookcover.archive.domain.report;

/**
 * 신고 처리 상태.
 * 대기 중, 승인됨, 거부됨 상태를 지원합니다.
 */
public enum ReportStatus {
    /** 대기 중 - 아직 검토되지 않음 */
    PENDING,
    /** 승인됨 - 신고가 유효하여 조치됨 */
    APPROVED,
    /** 거부됨 - 신고가 유효하지 않음 */
    REJECTED
}
