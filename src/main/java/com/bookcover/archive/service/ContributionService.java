package com.bookcover.archive.service;

import com.bookcover.archive.domain.user.User;

import java.util.UUID;

/**
 * 기여 관리 서비스 인터페이스.
 * 회원의 등록 기여 횟수를 추적하고 칭호를 관리합니다.
 *
 * Requirements:
 * - 7.5: 일반 회원이 10권 이상의 책을 아카이브에 등록하면 칭호를 "컨트리뷰터"로 자동 변경
 * - 7.6: 회원의 등록 기여 횟수를 추적하고 저장
 */
public interface ContributionService {

    /**
     * 회원의 기여 횟수를 증가시키고 칭호를 업데이트합니다.
     *
     * @param userId 회원 ID
     * @return 업데이트된 회원 정보
     */
    User incrementContribution(UUID userId);

    /**
     * 회원의 기여 횟수를 감소시킵니다.
     *
     * @param userId 회원 ID
     * @return 업데이트된 회원 정보
     */
    User decrementContribution(UUID userId);

    /**
     * 회원의 기여 횟수를 재계산합니다.
     * 원서, 번역서, 리디자인 등록 수를 합산합니다.
     *
     * @param userId 회원 ID
     * @return 업데이트된 회원 정보
     */
    User recalculateContribution(UUID userId);

    /**
     * 회원의 칭호를 업데이트합니다.
     * 기여 횟수가 10 이상이면 CONTRIBUTOR로 변경합니다.
     *
     * @param userId 회원 ID
     * @return 업데이트된 회원 정보
     */
    User updateTitle(UUID userId);

    /**
     * 회원의 기여 횟수를 조회합니다.
     *
     * @param userId 회원 ID
     * @return 기여 횟수
     */
    int getContributionCount(UUID userId);
}
