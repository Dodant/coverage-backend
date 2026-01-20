package com.bookcover.archive.controller.dto.admin;

import com.bookcover.archive.domain.takedown.TakedownRequest;
import com.bookcover.archive.domain.takedown.TakedownStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * 저작권 삭제 요청 응답 DTO.
 */
public record TakedownResponse(
        UUID id,
        String requesterName,
        String requesterEmail,
        String requesterOrganization,
        String targetUrl,
        UUID targetId,
        String reason,
        String evidenceUrl,
        String evidenceDescription,
        TakedownStatus status,
        UUID reviewedById,
        String reviewedByName,
        String reviewNote,
        Instant reviewedAt,
        Boolean preventReupload,
        Instant createdAt
) {
    public static TakedownResponse from(TakedownRequest request) {
        return new TakedownResponse(
                request.getId(),
                request.getRequesterName(),
                request.getRequesterEmail(),
                request.getRequesterOrganization(),
                request.getTargetUrl(),
                request.getTargetId(),
                request.getReason(),
                request.getEvidenceUrl(),
                request.getEvidenceDescription(),
                request.getStatus(),
                request.getReviewedBy() != null ? request.getReviewedBy().getId() : null,
                request.getReviewedBy() != null ? request.getReviewedBy().getDisplayName() : null,
                request.getReviewNote(),
                request.getReviewedAt(),
                request.getPreventReupload(),
                request.getCreatedAt()
        );
    }
}
