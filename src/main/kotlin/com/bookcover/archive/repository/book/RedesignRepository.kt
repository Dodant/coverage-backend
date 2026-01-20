package com.bookcover.archive.repository.book

import com.bookcover.archive.domain.book.Redesign
import com.bookcover.archive.domain.common.ContentStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Repository for Redesign entity operations.
 *
 * Requirements:
 * - 3.4: List all redesigns linked to an original/translated book
 * - 3.5: Multiple users can upload redesigns for the same book
 * - 3.6: One user can upload multiple redesigns for the same book
 */
@Repository
interface RedesignRepository : JpaRepository<Redesign, UUID> {

    /**
     * Finds all redesigns for an original book.
     * Requirement 3.4: List all redesigns for a book
     * @param originalBookId The original book's UUID
     * @param pageable Pagination information
     * @return Page of redesigns for the original book
     */
    fun findByOriginalBookId(originalBookId: UUID, pageable: Pageable): Page<Redesign>

    /**
     * Finds all redesigns for a translated book.
     * Requirement 3.4: List all redesigns for a book
     * @param translatedBookId The translated book's UUID
     * @param pageable Pagination information
     * @return Page of redesigns for the translated book
     */
    fun findByTranslatedBookId(translatedBookId: UUID, pageable: Pageable): Page<Redesign>

    /**
     * Finds all redesigns created by a specific user.
     * @param createdById The user's UUID
     * @param pageable Pagination information
     * @return Page of redesigns by the user
     */
    fun findByCreatedById(createdById: UUID, pageable: Pageable): Page<Redesign>

    /**
     * Finds all redesigns by status.
     * @param status The content status
     * @param pageable Pagination information
     * @return Page of redesigns with the status
     */
    fun findByStatus(status: ContentStatus, pageable: Pageable): Page<Redesign>

    /**
     * Finds all published redesigns for an original book.
     * @param originalBookId The original book's UUID
     * @param status The content status
     * @param pageable Pagination information
     * @return Page of redesigns
     */
    fun findByOriginalBookIdAndStatus(
        originalBookId: UUID,
        status: ContentStatus,
        pageable: Pageable
    ): Page<Redesign>

    /**
     * Finds all published redesigns for a translated book.
     * @param translatedBookId The translated book's UUID
     * @param status The content status
     * @param pageable Pagination information
     * @return Page of redesigns
     */
    fun findByTranslatedBookIdAndStatus(
        translatedBookId: UUID,
        status: ContentStatus,
        pageable: Pageable
    ): Page<Redesign>

    /**
     * Counts redesigns for an original book.
     * @param originalBookId The original book's UUID
     * @return Count of redesigns
     */
    fun countByOriginalBookId(originalBookId: UUID): Long

    /**
     * Counts redesigns for a translated book.
     * @param translatedBookId The translated book's UUID
     * @return Count of redesigns
     */
    fun countByTranslatedBookId(translatedBookId: UUID): Long

    /**
     * Counts redesigns by a user.
     * @param createdById The user's UUID
     * @return Count of redesigns
     */
    fun countByCreatedById(createdById: UUID): Long

    /**
     * Finds redesigns by user for a specific original book.
     * @param createdById The user's UUID
     * @param originalBookId The original book's UUID
     * @return List of redesigns
     */
    fun findByCreatedByIdAndOriginalBookId(createdById: UUID, originalBookId: UUID): List<Redesign>

    /**
     * Finds redesigns by user for a specific translated book.
     * @param createdById The user's UUID
     * @param translatedBookId The translated book's UUID
     * @return List of redesigns
     */
    fun findByCreatedByIdAndTranslatedBookId(createdById: UUID, translatedBookId: UUID): List<Redesign>
}
