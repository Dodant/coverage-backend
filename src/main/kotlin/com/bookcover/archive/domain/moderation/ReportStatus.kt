package com.bookcover.archive.domain.moderation

/**
 * Status of a content report in the moderation workflow.
 *
 * Requirements:
 * - 15.3: Track report processing status
 */
enum class ReportStatus {
    /** Report is awaiting review by moderator */
    PENDING,

    /** Report was reviewed and action was taken */
    APPROVED,

    /** Report was reviewed and dismissed */
    REJECTED
}
