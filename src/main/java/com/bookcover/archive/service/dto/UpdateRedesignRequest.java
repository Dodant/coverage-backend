package com.bookcover.archive.service.dto;

import jakarta.validation.constraints.Size;

/**
 * 리디자인 수정 요청 DTO.
 * 모든 필드는 선택적이며, 제공된 필드만 업데이트됩니다.
 */
public record UpdateRedesignRequest(
    String imageUrl,

    String thumbnailUrl,

    @Size(max = 2000, message = "설명은 2000자를 초과할 수 없습니다")
    String description
) {}
