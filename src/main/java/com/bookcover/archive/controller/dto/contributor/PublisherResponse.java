package com.bookcover.archive.controller.dto.contributor;

import com.bookcover.archive.domain.contributor.Publisher;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * 출판사 응답 DTO.
 * 출판사 정보를 반환합니다.
 *
 * Requirements: 10.1, 10.4
 */
public record PublisherResponse(
    UUID id,
    String name,
    Set<String> aliases,
    String description,
    String websiteUrl,
    String countryCode,
    Instant createdAt,
    Instant updatedAt
) {
    public static PublisherResponse from(Publisher publisher) {
        return new PublisherResponse(
            publisher.getId(),
            publisher.getName(),
            publisher.getAliases(),
            publisher.getDescription(),
            publisher.getWebsiteUrl(),
            publisher.getCountryCode(),
            publisher.getCreatedAt(),
            publisher.getUpdatedAt()
        );
    }
}
