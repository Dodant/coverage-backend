package com.bookcover.archive.controller.dto.contributor;

import com.bookcover.archive.domain.contributor.Author;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * 작가 응답 DTO.
 * 작가 프로필 정보를 반환합니다.
 *
 * Requirements: 10.2, 10.4
 */
public record AuthorResponse(
    UUID id,
    String name,
    Set<String> aliases,
    String biography,
    String websiteUrl,
    String nationality,
    Instant createdAt,
    Instant updatedAt
) {
    public static AuthorResponse from(Author author) {
        return new AuthorResponse(
            author.getId(),
            author.getName(),
            author.getAliases(),
            author.getBiography(),
            author.getWebsiteUrl(),
            author.getNationality(),
            author.getCreatedAt(),
            author.getUpdatedAt()
        );
    }
}
