package com.bookcover.archive.domain.audit;

/**
 * 감사 로그 액션 유형.
 * 생성, 수정, 삭제, 병합, 분리, 상태 변경 등의 액션을 지원합니다.
 */
public enum AuditAction {
    /** 생성 */
    CREATE,
    /** 수정 */
    UPDATE,
    /** 삭제 */
    DELETE,
    /** 엔티티 병합 */
    MERGE,
    /** 엔티티 분리 */
    SPLIT,
    /** 상태 변경 */
    STATUS_CHANGE
}
