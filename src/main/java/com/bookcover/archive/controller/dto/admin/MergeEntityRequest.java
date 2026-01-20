package com.bookcover.archive.controller.dto.admin;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * 엔티티 병합 요청 DTO.
 */
public record MergeEntityRequest(
        @NotNull(message = "Source ID is required")
        UUID sourceId,
        @NotNull(message = "Target ID is required")
        UUID targetId
) {
}
