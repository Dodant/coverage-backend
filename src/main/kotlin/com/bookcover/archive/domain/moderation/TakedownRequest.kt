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
 * Represents a copyright takedown request from a rights holder.
 * Used to process DMCA-style takedown requests for copyrighted content.
 *
 * Requirements:
 * - 17.1: Provide takedown request form for rights holders (publishers/agents)
 * - 17.2: Record requester info, target URL, reason, and evidence
 */
@Entity
@Table(name = "takedown_requests")
class TakedownRequest(
    /** Name of the person or organization requesting the takedown */
    @Column(name = "requester_name", nullable = false)
    val requesterName: String,

    /** Email address of the requester for communication */
    @Column(name = "requester_email", nullable = false)
    val requesterEmail: String,

    /** Organization the requester represents (e.g., publisher name) */
    @Column(name = "requester_organization")
    val requesterOrganization: String? = null,

    /** Role of the requester (e.g., "Publisher", "Author", "Legal Representative") */
    @Column(name = "requester_role")
    val requesterRole: String? = null,

    /** Type of content being requested for takedown */
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    val targetType: ReportTargetType,

    /** UUID of the content being requested for takedown */
    @Column(name = "target_id", nullable = false)
    val targetId: UUID,

    /** URL of the content (for reference) */
    @Column(name = "target_url", nullable = false)
    val targetUrl: String,

    /** Detailed reason for the takedown request */
    @Column(name = "reason", nullable = false, length = 2000)
    val reason: String,

    /** URL or description of evidence supporting the claim */
    @Column(name = "evidence", length = 2000)
    val evidence: String? = null,

    /** Current status of the takedown request */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: TakedownStatus = TakedownStatus.PENDING,

    /** UUID of the admin who reviewed this request */
    @Column(name = "reviewed_by_id")
    var reviewedById: UUID? = null,

    /** Note from the reviewer explaining the decision */
    @Column(name = "review_note", length = 1000)
    var reviewNote: String? = null,

    /** Timestamp when the request was reviewed */
    @Column(name = "reviewed_at")
    var reviewedAt: Instant? = null,

    /** Flag to prevent re-upload of the taken down content */
    @Column(name = "prevent_reupload", nullable = false)
    var preventReupload: Boolean = false
) : BaseEntity() {

    /**
     * Checks if this request is still pending review.
     * @return true if the request has not been reviewed yet
     */
    fun isPending(): Boolean = status == TakedownStatus.PENDING

    /**
     * Checks if this request has been approved.
     * @return true if the request was approved
     */
    fun isApproved(): Boolean = status == TakedownStatus.APPROVED

    /**
     * Checks if this request has been rejected.
     * @return true if the request was rejected
     */
    fun isRejected(): Boolean = status == TakedownStatus.REJECTED

    /**
     * Checks if this request has been withdrawn.
     * @return true if the request was withdrawn
     */
    fun isWithdrawn(): Boolean = status == TakedownStatus.WITHDRAWN

    /**
     * Approves the takedown request and records the review details.
     * @param reviewerId UUID of the admin approving the request
     * @param note Optional note explaining the decision
     * @param preventReupload Whether to prevent re-upload of the content
     */
    fun approve(reviewerId: UUID, note: String? = null, preventReupload: Boolean = true) {
        status = TakedownStatus.APPROVED
        reviewedById = reviewerId
        reviewNote = note
        reviewedAt = Instant.now()
        this.preventReupload = preventReupload
    }

    /**
     * Rejects the takedown request and records the review details.
     * @param reviewerId UUID of the admin rejecting the request
     * @param note Note explaining the rejection reason (required for transparency)
     */
    fun reject(reviewerId: UUID, note: String) {
        status = TakedownStatus.REJECTED
        reviewedById = reviewerId
        reviewNote = note
        reviewedAt = Instant.now()
    }

    /**
     * Withdraws the takedown request (by the requester).
     */
    fun withdraw() {
        require(isPending()) { "Only pending requests can be withdrawn" }
        status = TakedownStatus.WITHDRAWN
    }

    /**
     * Checks if this request is for an original book.
     * @return true if the target is an original book
     */
    fun isForOriginalBook(): Boolean = targetType == ReportTargetType.ORIGINAL_BOOK

    /**
     * Checks if this request is for a translated book.
     * @return true if the target is a translated book
     */
    fun isForTranslatedBook(): Boolean = targetType == ReportTargetType.TRANSLATED_BOOK

    /**
     * Checks if this request is for a redesign.
     * @return true if the target is a redesign
     */
    fun isForRedesign(): Boolean = targetType == ReportTargetType.REDESIGN
}
