package com.bookcover.archive.domain.moderation

/**
 * Reasons for reporting content.
 *
 * Requirements:
 * - 15.3: Record report reason when content is reported
 */
enum class ReportReason {
    /** Content is inappropriate or offensive */
    INAPPROPRIATE,

    /** Content violates copyright */
    COPYRIGHT,

    /** Content is spam or promotional */
    SPAM,

    /** Content contains incorrect information */
    WRONG_INFO,

    /** Other reason (requires description) */
    OTHER
}
