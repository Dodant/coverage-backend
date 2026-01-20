package com.bookcover.archive.domain.moderation

/**
 * Types of actions that are recorded in the audit log.
 *
 * Requirements:
 * - 16.1: Track all data changes including create, update, delete operations
 * - 16.3: Track entity merge operations
 * - 16.4: Track entity split operations
 */
enum class AuditAction {
    /** A new entity was created */
    CREATE,

    /** An existing entity was updated */
    UPDATE,

    /** An entity was deleted */
    DELETE,

    /** Two entities were merged into one */
    MERGE,

    /** An entity was split into multiple entities */
    SPLIT,

    /** An entity's status was changed (e.g., published, hidden, deleted) */
    STATUS_CHANGE
}
