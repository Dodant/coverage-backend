package com.bookcover.archive.service;

import java.util.UUID;

/**
 * 레이트 리밋 서비스 인터페이스.
 * Redis 기반으로 사용자별 액션 제한을 관리합니다.
 *
 * Requirements:
 * - 18.1: 댓글, 좋아요, 신고, 업로드에 대해 사용자 단위 레이트 리밋을 적용한다
 * - 18.5: 레이트 리밋에 도달하면 사용자에게 대기 시간을 안내한다
 */
public interface RateLimitService {

    /**
     * 사용자의 특정 액션에 대한 레이트 리밋을 체크합니다.
     *
     * @param userId 사용자 ID
     * @param action 액션 유형
     * @return 레이트 리밋 체크 결과
     */
    RateLimitResult checkLimit(UUID userId, RateLimitAction action);

    /**
     * 사용자의 특정 액션을 기록합니다.
     *
     * @param userId 사용자 ID
     * @param action 액션 유형
     */
    void recordAction(UUID userId, RateLimitAction action);

    /**
     * 사용자가 제한 상태인지 확인합니다.
     *
     * @param userId 사용자 ID
     * @return 제한 상태 여부
     */
    boolean isUserRestricted(UUID userId);

    /**
     * 레이트 리밋을 체크하고 허용되면 액션을 기록합니다.
     * 허용되지 않으면 RateLimitExceededException을 발생시킵니다.
     *
     * @param userId 사용자 ID
     * @param action 액션 유형
     * @return 레이트 리밋 체크 결과
     * @throws com.bookcover.archive.exception.RateLimitExceededException 레이트 리밋 초과 시
     */
    RateLimitResult checkAndRecord(UUID userId, RateLimitAction action);
}
