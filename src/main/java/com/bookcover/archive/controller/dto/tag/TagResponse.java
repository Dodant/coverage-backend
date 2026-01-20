package com.bookcover.archive.controller.dto.tag;

import com.bookcover.archive.domain.tag.DesignTag;
import com.bookcover.archive.domain.tag.TagCategory;

import java.time.Instant;
import java.util.UUID;

/**
 * 태그 응답 DTO.
 * 디자인 태그 정보를 반환합니다.
 *
 * Requirements: 20.3, 20.6
 */
public record TagResponse(
    UUID id,
    String name,
    TagCategory category,
    Integer usageCount,
    Boolean approved,
    Instant createdAt
) {
    public static TagResponse from(DesignTag tag) {
        return new TagResponse(
            tag.getId(),
            tag.getName(),
            tag.getCategory(),
            tag.getUsageCount(),
            tag.getApproved(),
            tag.getCreatedAt()
        );
    }
}
