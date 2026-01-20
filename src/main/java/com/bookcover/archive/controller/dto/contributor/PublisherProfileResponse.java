package com.bookcover.archive.controller.dto.contributor;

import com.bookcover.archive.domain.contributor.Publisher;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * 출판사 프로필 응답 DTO.
 * 출판사 프로필 정보와 관련 북커버 수를 반환합니다.
 *
 * Requirements: 10.4, 10.5
 */
public record PublisherProfileResponse(
    UUID id,
    String name,
    Set<String> aliases,
    String description,
    String websiteUrl,
    String countryCode,
    long bookCount,
    Instant createdAt,
    Instant updatedAt
) {
    public static PublisherProfileResponse from(Publisher publisher, long bookCount) {
        return new PublisherProfileResponse(
            publisher.getId(),
            publisher.getName(),
            publisher.getAliases(),
            publisher.getDescription(),
            publisher.getWebsiteUrl(),
            publisher.getCountryCode(),
            bookCount,
            publisher.getCreatedAt(),
            publisher.getUpdatedAt()
        );
    }
}
