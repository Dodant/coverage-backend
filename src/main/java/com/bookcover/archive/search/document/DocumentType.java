package com.bookcover.archive.search.document;

/**
 * Elasticsearch 문서 유형.
 * 원서, 번역서, 리디자인을 구분합니다.
 */
public enum DocumentType {
    /** 원서 */
    ORIGINAL,
    /** 번역서 */
    TRANSLATED,
    /** 리디자인 */
    REDESIGN
}
