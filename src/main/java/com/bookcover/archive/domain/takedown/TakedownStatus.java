package com.bookcover.archive.domain.takedown;

/**
 * 저작권 삭제 요청 처리 상태.
 * 대기 중, 승인됨, 거부됨 상태를 지원합니다.
 */
public enum TakedownStatus {
    /** 대기 중 - 아직 검토되지 않음 */
    PENDING,
    /** 승인됨 - 삭제 요청이 승인되어 콘텐츠가 비공개 처리됨 */
    APPROVED,
    /** 거부됨 - 삭제 요청이 유효하지 않음 */
    REJECTED
}
