package com.bookcover.archive.controller.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 사용자 정지 요청 DTO.
 */
public record SuspendUserRequest(
        @NotNull(message = "Duration is required")
        @Min(value = 1, message = "Duration must be at least 1 minute")
        Long durationMinutes,
        String reason
) {
}
