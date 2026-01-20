package com.bookcover.archive.controller.dto.user;

import com.bookcover.archive.domain.user.User;
import com.bookcover.archive.domain.user.UserRole;
import com.bookcover.archive.domain.user.UserTitle;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * 회원 프로필 응답 DTO.
 *
 * Requirements: 7.7 - 역할, 칭호, 기여 횟수, 웹사이트, 포트폴리오, SNS 정보 표시
 */
public record UserProfileResponse(
        UUID id,
        String email,
        String displayName,
        UserRole role,
        UserTitle title,
        String websiteUrl,
        String portfolioUrl,
        Map<String, String> snsAccounts,
        Integer contributionCount,
        Instant createdAt,
        Instant lastLoginAt
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                user.getTitle(),
                user.getWebsiteUrl(),
                user.getPortfolioUrl(),
                user.getSnsAccounts(),
                user.getContributionCount(),
                user.getCreatedAt(),
                user.getLastLoginAt()
        );
    }

    /**
     * 다른 사용자가 볼 수 있는 공개 프로필 정보만 포함합니다.
     */
    public static UserProfileResponse publicFrom(User user) {
        return new UserProfileResponse(
                user.getId(),
                null, // 이메일은 비공개
                user.getDisplayName(),
                user.getRole(),
                user.getTitle(),
                user.getWebsiteUrl(),
                user.getPortfolioUrl(),
                user.getSnsAccounts(),
                user.getContributionCount(),
                user.getCreatedAt(),
                null // 마지막 로그인 시간은 비공개
        );
    }
}
