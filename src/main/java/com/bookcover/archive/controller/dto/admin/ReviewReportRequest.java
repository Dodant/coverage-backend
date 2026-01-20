package com.bookcover.archive.controller.dto.admin;

import com.bookcover.archive.domain.report.ReportStatus;
import jakarta.validation.constraints.NotNull;

/**
 * 신고 검토 요청 DTO.
 */
public record ReviewReportRequest(
        @NotNull(message = "Status is required")
        ReportStatus status,
        String note
) {
}
