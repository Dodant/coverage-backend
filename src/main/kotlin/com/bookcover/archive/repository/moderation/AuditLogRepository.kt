package com.bookcover.archive.repository.moderation

import com.bookcover.archive.domain.moderation.AuditAction
import com.bookcover.archive.domain.moderation.AuditEntityType
import com.bookcover.archive.domain.moderation.AuditLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

/**
 * Repository for AuditLog entity operations.
 *
 * Requirements:
 * - 16.1: Record before/after values, modifier, and timestamp
 * - 16.2: View modification history on entity detail pages
 * - 16.6: Retain audit logs for at least 1 year
 */
@Repository
interface AuditLogRepository : JpaRepository<AuditLog, UUID> {

    /**
     * Finds all audit logs for a specific entity.
     * Requirement 16.2: View modification history
     * @param entityType The type of entity
     * @param entityId The entity's UUID
     * @param pageable Pagination information
     * @return Page of audit logs
     */
    fun findByEntityTypeAndEntityId(
        entityType: AuditEntityType,
        entityId: UUID,
        pageable: Pageable
    ): Page<AuditLog>

    /**
     * Finds all audit logs for a specific entity ordered by creation time.
     * @param entityType The type of entity
     * @param entityId The entity's UUID
     * @return List of audit logs in chronological order
     */
    fun findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
        entityType: AuditEntityType,
        entityId: UUID
    ): List<AuditLog>

    /**
     * Finds all audit logs by action type.
     * @param action The audit action
     * @param pageable Pagination information
     * @return Page of audit logs
     */
    fun findByAction(action: AuditAction, pageable: Pageable): Page<AuditLog>

    /**
     * Finds all audit logs by entity type.
     * @param entityType The type of entity
     * @param pageable Pagination information
     * @return Page of audit logs
     */
    fun findByEntityType(entityType: AuditEntityType, pageable: Pageable): Page<AuditLog>

    /**
     * Finds all audit logs performed by a specific user.
     * @param performedById The user's UUID
     * @param pageable Pagination information
     * @return Page of audit logs
     */
    fun findByPerformedById(performedById: UUID, pageable: Pageable): Page<AuditLog>

    /**
     * Finds audit logs within a time range.
     * @param startTime The start of the time range
     * @param endTime The end of the time range
     * @param pageable Pagination information
     * @return Page of audit logs
     */
    fun findByCreatedAtBetween(startTime: Instant, endTime: Instant, pageable: Pageable): Page<AuditLog>

    /**
     * Finds audit logs for an entity within a time range.
     * @param entityType The type of entity
     * @param entityId The entity's UUID
     * @param startTime The start of the time range
     * @param endTime The end of the time range
     * @param pageable Pagination information
     * @return Page of audit logs
     */
    fun findByEntityTypeAndEntityIdAndCreatedAtBetween(
        entityType: AuditEntityType,
        entityId: UUID,
        startTime: Instant,
        endTime: Instant,
        pageable: Pageable
    ): Page<AuditLog>

    /**
     * Counts audit logs for a specific entity.
     * @param entityType The type of entity
     * @param entityId The entity's UUID
     * @return Count of audit logs
     */
    fun countByEntityTypeAndEntityId(entityType: AuditEntityType, entityId: UUID): Long

    /**
     * Counts audit logs by action type.
     * @param action The audit action
     * @return Count of audit logs
     */
    fun countByAction(action: AuditAction): Long

    /**
     * Deletes audit logs older than a specific date.
     * Requirement 16.6: Retention policy (called by scheduled cleanup)
     * @param cutoffDate The cutoff date
     * @return Number of deleted records
     */
    fun deleteByCreatedAtBefore(cutoffDate: Instant): Long

    /**
     * Finds the most recent audit log for an entity.
     * @param entityType The type of entity
     * @param entityId The entity's UUID
     * @return The most recent audit log if found
     */
    fun findFirstByEntityTypeAndEntityIdOrderByCreatedAtDesc(
        entityType: AuditEntityType,
        entityId: UUID
    ): AuditLog?
}
