package com.bookcover.archive.controller.dto.contributor;

import com.bookcover.archive.domain.contributor.Designer;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * 디자이너 프로필 응답 DTO.
 * 디자이너 프로필 정보와 크레딧에 오른 표지 수를 반환합니다.
 *
 * Requirements: 10.4, 10.5
 */
public record DesignerProfileResponse(
    UUID id,
    String name,
    Set<String> aliases,
    String biography,
    String websiteUrl,
    String portfolioUrl,
    String nationality,
    long coverCount,
    Instant createdAt,
    Instant updatedAt
) {
    public static DesignerProfileResponse from(Designer designer, long coverCount) {
        return new DesignerProfileResponse(
            designer.getId(),
            designer.getName(),
            designer.getAliases(),
            designer.getBiography(),
            designer.getWebsiteUrl(),
            designer.getPortfolioUrl(),
            designer.getNationality(),
            coverCount,
            designer.getCreatedAt(),
            designer.getUpdatedAt()
        );
    }
}
