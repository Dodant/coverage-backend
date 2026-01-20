package com.bookcover.archive.controller.dto.admin;

import com.bookcover.archive.domain.audit.AuditAction;
import com.bookcover.archive.domain.audit.AuditEntityType;
import com.bookcover.archive.domain.audit.AuditLog;

import java.time.Instant;
import java.util.UUID;

/**
 * 감사 로그 응답 DTO.
 */
public record AuditLogResponse(
        UUID id,
        AuditAction action,
        AuditEntityType entityType,
        UUID entityId,
        UUID performedById,
        String performedByName,
        String previousValue,
        String newValue,
        Instant createdAt
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getPerformedBy().getId(),
                log.getPerformedBy().getDisplayName(),
                log.getPreviousValue(),
                log.getNewValue(),
                log.getCreatedAt()
        );
    }
}
