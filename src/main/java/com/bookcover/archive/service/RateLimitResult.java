package com.bookcover.archive.service;

import java.time.Instant;

/**
 * 레이트 리밋 체크 결과.
 *
 * Requirements:
 * - 18.5: 레이트 리밋에 도달하면 사용자에게 대기 시간을 안내한다
 *
 * @param allowed 요청 허용 여부
 * @param remainingAttempts 남은 시도 횟수
 * @param resetTime 리밋 초기화 시간
 */
public record RateLimitResult(
    boolean allowed,
    int remainingAttempts,
    Instant resetTime
) {
    /**
     * 허용된 결과를 생성합니다.
     */
    public static RateLimitResult allowed(int remainingAttempts, Instant resetTime) {
        return new RateLimitResult(true, remainingAttempts, resetTime);
    }

    /**
     * 거부된 결과를 생성합니다.
     */
    public static RateLimitResult denied(Instant resetTime) {
        return new RateLimitResult(false, 0, resetTime);
    }
}
