package com.bookcover.archive.domain.moderation

import com.bookcover.archive.domain.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * Represents a user report against content in the archive system.
 * Reports are used to flag inappropriate, copyright-infringing, or incorrect content.
 *
 * Requirements:
 * - 15.2: Users can report inappropriate content
 * - 15.3: Record report reason, reporter, and timestamp
 */
@Entity
@Table(name = "reports")
class Report(
    /** UUID of the user who submitted this report */
    @Column(name = "reporter_id", nullable = false)
    val reporterId: UUID,

    /** Type of content being reported */
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    val targetType: ReportTargetType,

    /** UUID of the content being reported */
    @Column(name = "target_id", nullable = false)
    val targetId: UUID,

    /** Reason for the report */
    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false)
    val reason: ReportReason,

    /** Optional detailed description of the report */
    @Column(name = "description", length = 1000)
    val description: String? = null,

    /** Current status of the report */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: ReportStatus = ReportStatus.PENDING,

    /** UUID of the admin who reviewed this report */
    @Column(name = "reviewed_by_id")
    var reviewedById: UUID? = null,

    /** Note from the reviewer explaining the decision */
    @Column(name = "review_note", length = 1000)
    var reviewNote: String? = null,

    /** Timestamp when the report was reviewed */
    @Column(name = "reviewed_at")
    var reviewedAt: Instant? = null
) : BaseEntity() {

    /**
     * Checks if this report is still pending review.
     * @return true if the report has not been reviewed yet
     */
    fun isPending(): Boolean = status == ReportStatus.PENDING

    /**
     * Checks if this report has been approved (action taken).
     * @return true if the report was approved
     */
    fun isApproved(): Boolean = status == ReportStatus.APPROVED

    /**
     * Checks if this report has been rejected (dismissed).
     * @return true if the report was rejected
     */
    fun isRejected(): Boolean = status == ReportStatus.REJECTED

    /**
     * Approves the report and records the review details.
     * @param reviewerId UUID of the admin approving the report
     * @param note Optional note explaining the decision
     */
    fun approve(reviewerId: UUID, note: String? = null) {
        status = ReportStatus.APPROVED
        reviewedById = reviewerId
        reviewNote = note
        reviewedAt = Instant.now()
    }

    /**
     * Rejects the report and records the review details.
     * @param reviewerId UUID of the admin rejecting the report
     * @param note Optional note explaining the decision
     */
    fun reject(reviewerId: UUID, note: String? = null) {
        status = ReportStatus.REJECTED
        reviewedById = reviewerId
        reviewNote = note
        reviewedAt = Instant.now()
    }

    /**
     * Checks if this report is for an original book.
     * @return true if the target is an original book
     */
    fun isForOriginalBook(): Boolean = targetType == ReportTargetType.ORIGINAL_BOOK

    /**
     * Checks if this report is for a translated book.
     * @return true if the target is a translated book
     */
    fun isForTranslatedBook(): Boolean = targetType == ReportTargetType.TRANSLATED_BOOK

    /**
     * Checks if this report is for a redesign.
     * @return true if the target is a redesign
     */
    fun isForRedesign(): Boolean = targetType == ReportTargetType.REDESIGN

    /**
     * Checks if this report is for a comment.
     * @return true if the target is a comment
     */
    fun isForComment(): Boolean = targetType == ReportTargetType.COMMENT
}
