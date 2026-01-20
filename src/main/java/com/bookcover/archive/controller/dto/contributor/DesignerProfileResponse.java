package com.bookcover.archive.controller.dto.contributor;

import com.bookcover.archive.domain.contributor.Designer;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * 디자이너 프로필 응답 DTO.
 * 디자이너 프로필 정보와 관련 북커버 수를 반환합니다.
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
    long bookCount,
    Instant createdAt,
    Instant updatedAt
) {
    public static DesignerProfileResponse from(Designer designer, long bookCount) {
        return new DesignerProfileResponse(
            designer.getId(),
            designer.getName(),
            designer.getAliases(),
            designer.getBiography(),
            designer.getWebsiteUrl(),
            designer.getPortfolioUrl(),
            designer.getNationality(),
            bookCount,
            designer.getCreatedAt(),
            designer.getUpdatedAt()
        );
    }
}
