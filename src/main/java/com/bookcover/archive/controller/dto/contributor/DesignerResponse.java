package com.bookcover.archive.controller.dto.contributor;

import com.bookcover.archive.domain.contributor.Designer;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * 디자이너 응답 DTO.
 * 디자이너 정보를 반환합니다.
 *
 * Requirements: 10.3, 10.4
 */
public record DesignerResponse(
    UUID id,
    String name,
    Set<String> aliases,
    String biography,
    String websiteUrl,
    String portfolioUrl,
    String nationality,
    Instant createdAt,
    Instant updatedAt
) {
    public static DesignerResponse from(Designer designer) {
        return new DesignerResponse(
            designer.getId(),
            designer.getName(),
            designer.getAliases(),
            designer.getBiography(),
            designer.getWebsiteUrl(),
            designer.getPortfolioUrl(),
            designer.getNationality(),
            designer.getCreatedAt(),
            designer.getUpdatedAt()
        );
    }
}
