package com.bookcover.archive.domain.book;

/** 같은 판 안에서의 표지 변형 종류. 판 정보 자체는 책의 edition에 기록한다. */
public enum CoverVariantType {
    UNKNOWN, INITIAL, RECOVER, SPECIAL, PRINTING, IMAGE_REPLACEMENT
}
