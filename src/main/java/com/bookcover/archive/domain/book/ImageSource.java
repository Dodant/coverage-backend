package com.bookcover.archive.domain.book;

/**
 * 이미지 출처를 나타내는 열거형.
 */
public enum ImageSource {
    /** 스캔 이미지 */
    SCAN,
    /** 촬영 이미지 */
    PHOTO,
    /** 링크에서 가져온 이미지 */
    LINK,
    /** 기타 출처 */
    OTHER
}
