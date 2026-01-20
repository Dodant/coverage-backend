package com.bookcover.archive.domain.moderation

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

/**
 * Represents an audit log entry tracking changes to entities in the system.
 * Stores before/after values as JSON for complete change history.
 *
 * Requirements:
 * - 16.1: Record before/after values, modifier, and timestamp for all changes
 * - 16.6: Retain audit logs for at least 1 year
 *
 * Note: This entity does not extend BaseEntity because audit logs are immutable
 * and should not have updatedAt timestamps.
 */
@Entity
@Table(name = "audit_logs")
class AuditLog(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    /** The type of action performed */
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    val action: AuditAction,

    /** The type of entity that was modified */
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    val entityType: AuditEntityType,

    /** UUID of the entity that was modified */
    @Column(name = "entity_id", nullable = false)
    val entityId: UUID,

    /** UUID of the user who performed the action */
    @Column(name = "performed_by_id", nullable = false)
    val performedById: UUID,

    /** JSON representation of the entity state before the change (null for CREATE) */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "previous_value", columnDefinition = "jsonb")
    val previousValue: String? = null,

    /** JSON representation of the entity state after the change (null for DELETE) */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_value", columnDefinition = "jsonb")
    val newValue: String? = null,

    /** Timestamp when the action was performed */
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
) {

    /**
     * Checks if this is a create action.
     * @return true if the action is CREATE
     */
    fun isCreate(): Boolean = action == AuditAction.CREATE

    /**
     * Checks if this is an update action.
     * @return true if the action is UPDATE
     */
    fun isUpdate(): Boolean = action == AuditAction.UPDATE

    /**
     * Checks if this is a delete action.
     * @return true if the action is DELETE
     */
    fun isDelete(): Boolean = action == AuditAction.DELETE

    /**
     * Checks if this is a merge action.
     * @return true if the action is MERGE
     */
    fun isMerge(): Boolean = action == AuditAction.MERGE

    /**
     * Checks if this is a split action.
     * @return true if the action is SPLIT
     */
    fun isSplit(): Boolean = action == AuditAction.SPLIT

    /**
     * Checks if this is a status change action.
     * @return true if the action is STATUS_CHANGE
     */
    fun isStatusChange(): Boolean = action == AuditAction.STATUS_CHANGE

    /**
     * Checks if this log entry has previous value data.
     * @return true if previousValue is not null
     */
    fun hasPreviousValue(): Boolean = previousValue != null

    /**
     * Checks if this log entry has new value data.
     * @return true if newValue is not null
     */
    fun hasNewValue(): Boolean = newValue != null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AuditLog) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    companion object {
        /**
         * Creates an audit log entry for a create action.
         */
        fun forCreate(
            entityType: AuditEntityType,
            entityId: UUID,
            performedById: UUID,
            newValue: String
        ): AuditLog = AuditLog(
            action = AuditAction.CREATE,
            entityType = entityType,
            entityId = entityId,
            performedById = performedById,
            newValue = newValue
        )

        /**
         * Creates an audit log entry for an update action.
         */
        fun forUpdate(
            entityType: AuditEntityType,
            entityId: UUID,
            performedById: UUID,
            previousValue: String,
            newValue: String
        ): AuditLog = AuditLog(
            action = AuditAction.UPDATE,
            entityType = entityType,
            entityId = entityId,
            performedById = performedById,
            previousValue = previousValue,
            newValue = newValue
        )

        /**
         * Creates an audit log entry for a delete action.
         */
        fun forDelete(
            entityType: AuditEntityType,
            entityId: UUID,
            performedById: UUID,
            previousValue: String
        ): AuditLog = AuditLog(
            action = AuditAction.DELETE,
            entityType = entityType,
            entityId = entityId,
            performedById = performedById,
            previousValue = previousValue
        )

        /**
         * Creates an audit log entry for a status change action.
         */
        fun forStatusChange(
            entityType: AuditEntityType,
            entityId: UUID,
            performedById: UUID,
            previousValue: String,
            newValue: String
        ): AuditLog = AuditLog(
            action = AuditAction.STATUS_CHANGE,
            entityType = entityType,
            entityId = entityId,
            performedById = performedById,
            previousValue = previousValue,
            newValue = newValue
        )
    }
}
