package com.bookcover.archive.service

import com.bookcover.archive.domain.book.Redesign
import com.bookcover.archive.service.dto.CreateRedesignRequest
import com.bookcover.archive.service.dto.UpdateRedesignRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

/**
 * Service interface for redesign management operations.
 *
 * Requirements:
 * - 3.1: Redesign linked to original or translated book
 * - 3.2: Redesign image and description required
 * - 3.5: Multiple users can upload redesigns for the same book
 * - 3.6: One user can upload multiple redesigns for the same book
 */
interface RedesignService {

    /**
     * Creates a new redesign.
     * Requirements: 3.1, 3.2
     *
     * @param request The creation request containing redesign details
     * @param createdById The ID of the user creating the redesign
     * @return The created redesign
     * @throws ValidationException if required fields are missing or invalid
     * @throws ResourceNotFoundException if linked book not found
     */
    fun createRedesign(request: CreateRedesignRequest, createdById: UUID): Redesign

    /**
     * Updates an existing redesign.
     *
     * @param id The ID of the redesign to update
     * @param request The update request
     * @param userId The ID of the user making the update (for authorization)
     * @return The updated redesign
     * @throws ResourceNotFoundException if redesign not found
     * @throws UnauthorizedException if user is not the owner
     */
    fun updateRedesign(id: UUID, request: UpdateRedesignRequest, userId: UUID): Redesign

    /**
     * Gets a redesign by ID.
     *
     * @param id The redesign ID
     * @return The redesign or null if not found
     */
    fun getRedesign(id: UUID): Redesign?

    /**
     * Deletes a redesign (soft delete - sets status to DELETED).
     *
     * @param id The ID of the redesign to delete
     * @param userId The ID of the user making the deletion (for authorization)
     * @throws ResourceNotFoundException if redesign not found
     * @throws UnauthorizedException if user is not the owner
     */
    fun deleteRedesign(id: UUID, userId: UUID)

    /**
     * Gets all redesigns for an original book.
     * Requirements: 3.4
     *
     * @param originalBookId The original book ID
     * @param pageable Pagination information
     * @return Page of redesigns
     */
    fun getRedesignsByOriginalBook(originalBookId: UUID, pageable: Pageable): Page<Redesign>

    /**
     * Gets all redesigns for a translated book.
     * Requirements: 3.4
     *
     * @param translatedBookId The translated book ID
     * @param pageable Pagination information
     * @return Page of redesigns
     */
    fun getRedesignsByTranslatedBook(translatedBookId: UUID, pageable: Pageable): Page<Redesign>

    /**
     * Gets all redesigns by a user.
     *
     * @param userId The user ID
     * @param pageable Pagination information
     * @return Page of redesigns
     */
    fun getRedesignsByUser(userId: UUID, pageable: Pageable): Page<Redesign>

    /**
     * Counts redesigns for an original book.
     *
     * @param originalBookId The original book ID
     * @return Count of redesigns
     */
    fun countRedesignsByOriginalBook(originalBookId: UUID): Long

    /**
     * Counts redesigns for a translated book.
     *
     * @param translatedBookId The translated book ID
     * @return Count of redesigns
     */
    fun countRedesignsByTranslatedBook(translatedBookId: UUID): Long

    /**
     * Counts redesigns by a user.
     *
     * @param userId The user ID
     * @return Count of redesigns
     */
    fun countRedesignsByUser(userId: UUID): Long
}
