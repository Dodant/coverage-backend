package com.bookcover.archive.domain.moderation

/**
 * Types of content that can be reported by users.
 *
 * Requirements:
 * - 15.2: Users can report inappropriate content
 */
enum class ReportTargetType {
    /** Report against an original book entry */
    ORIGINAL_BOOK,

    /** Report against a translated book entry */
    TRANSLATED_BOOK,

    /** Report against a user-submitted redesign */
    REDESIGN,

    /** Report against a comment */
    COMMENT
}
