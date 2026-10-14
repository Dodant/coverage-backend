package com.bookcover.archive.service;

/**
 * 레이트 리밋 서비스 인터페이스.
 * Redis 기반으로 주체(로그인 사용자 또는 비로그인 IP)별 액션 제한을 관리합니다.
 *
 * Requirements:
 * - 18.1: 댓글, 좋아요, 신고, 업로드에 대해 사용자 단위 레이트 리밋을 적용한다
 * - 18.5: 레이트 리밋에 도달하면 사용자에게 대기 시간을 안내한다
 */
public interface RateLimitService {

    /**
     * 액션 1회를 원자적으로 기록하고, 한도를 넘었으면 RateLimitExceededException을 발생시킵니다.
     *
     * @param subject 제한 주체 키 (예: "user:{uuid}", "ip:{addr}")
     * @param action  액션 유형
     * @throws com.bookcover.archive.exception.RateLimitExceededException 레이트 리밋 초과 시
     */
    void checkAndRecord(String subject, RateLimitAction action);

    /**
     * 클라이언트 오류(4xx)로 끝난 요청의 기록 1회를 되돌립니다.
     * 윈도우당 한도의 3배까지만 환불하고, 그 이후 실패는 그대로 한도에 포함됩니다.
     * 이미 만료된 윈도우는 건드리지 않습니다.
     */
    void refund(String subject, RateLimitAction action);
}
