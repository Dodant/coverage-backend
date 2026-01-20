package com.bookcover.archive.domain.common

/**
 * Represents the publication status of content in the archive system.
 * Used across various entities like Media, Redesign, and Comment.
 */
enum class ContentStatus {
    /** Content is in draft state, not yet published */
    DRAFT,

    /** Content is published and visible to users */
    PUBLISHED,

    /** Content is hidden (e.g., due to reports or moderation) */
    HIDDEN,

    /** Content has been deleted */
    DELETED
}
