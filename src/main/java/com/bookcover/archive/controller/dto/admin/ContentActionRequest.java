package com.bookcover.archive.controller.dto.admin;

import com.bookcover.archive.domain.report.ReportTargetType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * 콘텐츠 관리 액션 요청 DTO.
 */
public record ContentActionRequest(
        @NotNull(message = "Target type is required")
        ReportTargetType targetType,
        @NotNull(message = "Target ID is required")
        UUID targetId,
        String reason
) {
}
