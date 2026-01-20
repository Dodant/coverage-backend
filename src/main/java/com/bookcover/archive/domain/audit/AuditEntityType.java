package com.bookcover.archive.domain.audit;

/**
 * 감사 로그 대상 엔티티 유형.
 * 원서, 번역서, 리디자인, 회원, 컬렉션, 작가, 출판사, 디자이너 등을 지원합니다.
 */
public enum AuditEntityType {
    /** 원서 */
    ORIGINAL_BOOK,
    /** 번역서 */
    TRANSLATED_BOOK,
    /** 리디자인 */
    REDESIGN,
    /** 회원 */
    USER,
    /** 컬렉션 */
    COLLECTION,
    /** 작가 */
    AUTHOR,
    /** 출판사 */
    PUBLISHER,
    /** 디자이너 */
    DESIGNER
}
