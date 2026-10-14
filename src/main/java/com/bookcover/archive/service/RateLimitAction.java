package com.bookcover.archive.service;

/**
 * 레이트 리밋이 적용되는 액션 유형.
 *
 * Requirements:
 * - 18.1: 댓글, 좋아요, 신고, 업로드에 대해 사용자 단위 레이트 리밋을 적용한다
 * - TAKEDOWN: 비회원 저작권 삭제 요청 (IP 단위)
 */
public enum RateLimitAction {
    UPLOAD,
    COMMENT,
    LIKE,
    REPORT,
    TAKEDOWN
}
