package com.bookcover.archive.domain.collection

/**
 * Represents the visibility setting of a collection.
 *
 * Requirements:
 * - 11.1: Collections can be public or private
 * - 11.10: Collections can be shared via link (read-only)
 */
enum class CollectionVisibility {
    /** Collection is only visible to the owner */
    PRIVATE,

    /** Collection is visible to all users */
    PUBLIC,

    /** Collection is only accessible via share link */
    LINK_ONLY
}
