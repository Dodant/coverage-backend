package com.bookcover.archive.controller.dto.contributor;

import com.bookcover.archive.domain.contributor.Author;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * 작가 프로필 응답 DTO.
 * 작가 프로필 정보와 관련 북커버 수를 반환합니다.
 *
 * Requirements: 10.4, 10.5
 */
public record AuthorProfileResponse(
    UUID id,
    String name,
    Set<String> aliases,
    String biography,
    String websiteUrl,
    String nationality,
    long bookCount,
    Instant createdAt,
    Instant updatedAt
) {
    public static AuthorProfileResponse from(Author author, long bookCount) {
        return new AuthorProfileResponse(
            author.getId(),
            author.getName(),
            author.getAliases(),
            author.getBiography(),
            author.getWebsiteUrl(),
            author.getNationality(),
            bookCount,
            author.getCreatedAt(),
            author.getUpdatedAt()
        );
    }
}
