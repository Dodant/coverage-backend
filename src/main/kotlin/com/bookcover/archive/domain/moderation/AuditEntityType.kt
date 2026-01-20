package com.bookcover.archive.domain.moderation

/**
 * Types of entities that can be tracked in the audit log.
 *
 * Requirements:
 * - 16.1: Track changes to books, redesigns, users, collections, and contributors
 */
enum class AuditEntityType {
    /** Original book entity */
    ORIGINAL_BOOK,

    /** Translated book entity */
    TRANSLATED_BOOK,

    /** User-submitted redesign */
    REDESIGN,

    /** User account */
    USER,

    /** User collection */
    COLLECTION,

    /** Author entity */
    AUTHOR,

    /** Publisher entity */
    PUBLISHER,

    /** Designer entity */
    DESIGNER
}
