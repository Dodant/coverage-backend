package com.bookcover.archive.repository.moderation

import com.bookcover.archive.domain.moderation.ReportTargetType
import com.bookcover.archive.domain.moderation.TakedownRequest
import com.bookcover.archive.domain.moderation.TakedownStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Repository for TakedownRequest entity operations.
 *
 * Requirements:
 * - 17.1: Takedown request form for rights holders
 * - 17.2: Record requester info, target URL, reason, and evidence
 * - 17.3: Approve takedown and set re-upload prevention flag
 * - 17.4: Reject takedown with reason notification
 * - 17.6: Maintain takedown request history
 */
@Repository
interface TakedownRequestRepository : JpaRepository<TakedownRequest, UUID> {

    /**
     * Finds all takedown requests by status.
     * @param status The request status
     * @param pageable Pagination information
     * @return Page of takedown requests
     */
    fun findByStatus(status: TakedownStatus, pageable: Pageable): Page<TakedownRequest>

    /**
     * Finds all pending takedown requests (for admin queue).
     * @param pageable Pagination information
     * @return Page of pending requests
     */
    fun findByStatusOrderByCreatedAtAsc(status: TakedownStatus, pageable: Pageable): Page<TakedownRequest>

    /**
     * Finds all takedown requests for specific content.
     * @param targetType The type of content
     * @param targetId The content's UUID
     * @param pageable Pagination information
     * @return Page of takedown requests
     */
    fun findByTargetTypeAndTargetId(
        targetType: ReportTargetType,
        targetId: UUID,
        pageable: Pageable
    ): Page<TakedownRequest>

    /**
     * Finds all takedown requests by requester email.
     * @param requesterEmail The requester's email
     * @param pageable Pagination information
     * @return Page of takedown requests
     */
    fun findByRequesterEmail(requesterEmail: String, pageable: Pageable): Page<TakedownRequest>

    /**
     * Finds all takedown requests by requester organization.
     * @param requesterOrganization The organization name
     * @param pageable Pagination information
     * @return Page of takedown requests
     */
    fun findByRequesterOrganization(requesterOrganization: String, pageable: Pageable): Page<TakedownRequest>

    /**
     * Checks if a takedown request exists for specific content.
     * @param targetType The type of content
     * @param targetId The content's UUID
     * @return true if a request exists
     */
    fun existsByTargetTypeAndTargetId(targetType: ReportTargetType, targetId: UUID): Boolean

    /**
     * Checks if an approved takedown exists for specific content with re-upload prevention.
     * @param targetType The type of content
     * @param targetId The content's UUID
     * @param status The request status
     * @param preventReupload Whether re-upload is prevented
     * @return true if such a request exists
     */
    fun existsByTargetTypeAndTargetIdAndStatusAndPreventReupload(
        targetType: ReportTargetType,
        targetId: UUID,
        status: TakedownStatus,
        preventReupload: Boolean
    ): Boolean

    /**
     * Counts takedown requests by status.
     * @param status The request status
     * @return Count of requests
     */
    fun countByStatus(status: TakedownStatus): Long

    /**
     * Finds takedown requests reviewed by a specific admin.
     * @param reviewedById The admin's UUID
     * @param pageable Pagination information
     * @return Page of takedown requests
     */
    fun findByReviewedById(reviewedById: UUID, pageable: Pageable): Page<TakedownRequest>

    /**
     * Finds approved takedown requests with re-upload prevention.
     * @param status The request status
     * @param preventReupload Whether re-upload is prevented
     * @param pageable Pagination information
     * @return Page of takedown requests
     */
    fun findByStatusAndPreventReupload(
        status: TakedownStatus,
        preventReupload: Boolean,
        pageable: Pageable
    ): Page<TakedownRequest>
}
