package com.bookcover.archive.repository.moderation

import com.bookcover.archive.domain.moderation.Report
import com.bookcover.archive.domain.moderation.ReportReason
import com.bookcover.archive.domain.moderation.ReportStatus
import com.bookcover.archive.domain.moderation.ReportTargetType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Repository for Report entity operations.
 *
 * Requirements:
 * - 15.2: Users can report inappropriate content
 * - 15.3: Record report reason, reporter, and timestamp
 * - 15.4: Auto-hide content after 3+ reports
 */
@Repository
interface ReportRepository : JpaRepository<Report, UUID> {

    /**
     * Finds all reports for specific content.
     * @param targetType The type of content
     * @param targetId The content's UUID
     * @param pageable Pagination information
     * @return Page of reports
     */
    fun findByTargetTypeAndTargetId(
        targetType: ReportTargetType,
        targetId: UUID,
        pageable: Pageable
    ): Page<Report>

    /**
     * Finds all reports by status.
     * @param status The report status
     * @param pageable Pagination information
     * @return Page of reports
     */
    fun findByStatus(status: ReportStatus, pageable: Pageable): Page<Report>

    /**
     * Finds all pending reports (for admin queue).
     * @param pageable Pagination information
     * @return Page of pending reports
     */
    fun findByStatusOrderByCreatedAtAsc(status: ReportStatus, pageable: Pageable): Page<Report>

    /**
     * Finds all reports by a reporter.
     * @param reporterId The reporter's UUID
     * @param pageable Pagination information
     * @return Page of reports
     */
    fun findByReporterId(reporterId: UUID, pageable: Pageable): Page<Report>

    /**
     * Finds all reports by reason.
     * @param reason The report reason
     * @param pageable Pagination information
     * @return Page of reports
     */
    fun findByReason(reason: ReportReason, pageable: Pageable): Page<Report>

    /**
     * Counts reports for specific content.
     * Requirement 15.4: Auto-hide after 3+ reports
     * @param targetType The type of content
     * @param targetId The content's UUID
     * @return Count of reports
     */
    fun countByTargetTypeAndTargetId(targetType: ReportTargetType, targetId: UUID): Long

    /**
     * Counts pending reports for specific content.
     * @param targetType The type of content
     * @param targetId The content's UUID
     * @param status The report status
     * @return Count of reports
     */
    fun countByTargetTypeAndTargetIdAndStatus(
        targetType: ReportTargetType,
        targetId: UUID,
        status: ReportStatus
    ): Long

    /**
     * Counts reports by a reporter.
     * @param reporterId The reporter's UUID
     * @return Count of reports
     */
    fun countByReporterId(reporterId: UUID): Long

    /**
     * Counts reports by status.
     * @param status The report status
     * @return Count of reports
     */
    fun countByStatus(status: ReportStatus): Long

    /**
     * Checks if a user has already reported specific content.
     * @param reporterId The reporter's UUID
     * @param targetType The type of content
     * @param targetId The content's UUID
     * @return true if the user has already reported this content
     */
    fun existsByReporterIdAndTargetTypeAndTargetId(
        reporterId: UUID,
        targetType: ReportTargetType,
        targetId: UUID
    ): Boolean

    /**
     * Finds reports reviewed by a specific admin.
     * @param reviewedById The admin's UUID
     * @param pageable Pagination information
     * @return Page of reports
     */
    fun findByReviewedById(reviewedById: UUID, pageable: Pageable): Page<Report>

    /**
     * Finds reports by target type and status.
     * @param targetType The type of content
     * @param status The report status
     * @param pageable Pagination information
     * @return Page of reports
     */
    fun findByTargetTypeAndStatus(
        targetType: ReportTargetType,
        status: ReportStatus,
        pageable: Pageable
    ): Page<Report>
}
