package com.bookcover.archive.controller.dto.admin;

import com.bookcover.archive.domain.report.Report;
import com.bookcover.archive.domain.report.ReportReason;
import com.bookcover.archive.domain.report.ReportStatus;
import com.bookcover.archive.domain.report.ReportTargetType;

import java.time.Instant;
import java.util.UUID;

/**
 * 신고 응답 DTO.
 */
public record ReportResponse(
        UUID id,
        UUID reporterId,
        String reporterName,
        ReportTargetType targetType,
        UUID targetId,
        ReportReason reason,
        String description,
        ReportStatus status,
        UUID reviewedById,
        String reviewedByName,
        String reviewNote,
        Instant reviewedAt,
        Instant createdAt
) {
    public static ReportResponse from(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getReporter().getId(),
                report.getReporter().getDisplayName(),
                report.getTargetType(),
                report.getTargetId(),
                report.getReason(),
                report.getDescription(),
                report.getStatus(),
                report.getReviewedBy() != null ? report.getReviewedBy().getId() : null,
                report.getReviewedBy() != null ? report.getReviewedBy().getDisplayName() : null,
                report.getReviewNote(),
                report.getReviewedAt(),
                report.getCreatedAt()
        );
    }
}
