package com.bookcover.archive.domain.report;

/**
 * 신고 사유.
 * 부적절한 콘텐츠, 저작권 침해, 스팸, 잘못된 정보 등의 사유를 지원합니다.
 */
public enum ReportReason {
    /** 부적절한 콘텐츠 */
    INAPPROPRIATE,
    /** 저작권 침해 */
    COPYRIGHT,
    /** 스팸 */
    SPAM,
    /** 잘못된 정보 */
    WRONG_INFO,
    /** 기타 */
    OTHER
}
