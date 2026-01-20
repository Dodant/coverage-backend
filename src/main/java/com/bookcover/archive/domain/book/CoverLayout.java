package com.bookcover.archive.domain.book;

/**
 * 북커버 레이아웃 유형을 나타내는 열거형.
 */
public enum CoverLayout {
    /** 중앙 정렬 */
    CENTER,
    /** 좌측 정렬 */
    LEFT,
    /** 우측 정렬 */
    RIGHT,
    /** 전면 출혈 (여백 없음) */
    FULL_BLEED,
    /** 미니멀 */
    MINIMAL
}
