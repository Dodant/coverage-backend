package com.bookcover.archive.controller.dto.book;

import com.bookcover.archive.domain.book.Redesign;
import com.bookcover.archive.domain.common.ContentStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * 리디자인 응답 DTO.
 */
public record RedesignResponse(
    UUID id,
    String imageUrl,
    String thumbnailUrl,
    String description,
    UUID originalBookId,
    String originalBookTitle,
    UUID translatedBookId,
    String translatedBookTitle,
    UserInfo createdBy,
    ContentStatus status,
    Integer likeCount,
    Integer commentCount,
    Instant createdAt,
    Instant updatedAt
) {
    public static RedesignResponse from(Redesign redesign) {
        return new RedesignResponse(
            redesign.getId(),
            redesign.getStatus() == ContentStatus.HIDDEN ? null : redesign.getImageUrl(),
            redesign.getStatus() == ContentStatus.HIDDEN ? null : redesign.getThumbnailUrl(),
            redesign.getDescription(),
            redesign.getOriginalBook() != null ? redesign.getOriginalBook().getId() : null,
            redesign.getOriginalBook() != null ? redesign.getOriginalBook().getTitle() : null,
            redesign.getTranslatedBook() != null ? redesign.getTranslatedBook().getId() : null,
            redesign.getTranslatedBook() != null ? redesign.getTranslatedBook().getTitle() : null,
            redesign.getCreatedBy() != null ? new UserInfo(redesign.getCreatedBy().getId(), redesign.getCreatedBy().getDisplayName()) : null,
            redesign.getStatus(),
            redesign.getLikeCount(),
            redesign.getCommentCount(),
            redesign.getCreatedAt(),
            redesign.getUpdatedAt()
        );
    }

    public record UserInfo(UUID id, String displayName) {}
}
