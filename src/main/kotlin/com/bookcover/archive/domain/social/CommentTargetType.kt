package com.bookcover.archive.domain.social

/**
 * Represents the type of content that can receive comments.
 * Supports polymorphic comments across different content types.
 *
 * Requirements:
 * - 5.3: Independent comment functionality for original covers, translated covers, and redesigns
 */
enum class CommentTargetType {
    /** Comment on an original book's cover */
    ORIGINAL_COVER,

    /** Comment on a translated book's cover */
    TRANSLATED_COVER,

    /** Comment on a user's redesign */
    REDESIGN
}
