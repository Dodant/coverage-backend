package com.bookcover.archive.domain.social

/**
 * Represents the type of content that can receive likes.
 * Supports polymorphic likes across different content types.
 *
 * Requirements:
 * - 4.3: Independent like functionality for original covers, translated covers, and redesigns
 */
enum class LikeTargetType {
    /** Like on an original book's cover */
    ORIGINAL_COVER,

    /** Like on a translated book's cover */
    TRANSLATED_COVER,

    /** Like on a user's redesign */
    REDESIGN
}
