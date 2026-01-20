package com.bookcover.archive.repository.collection

import com.bookcover.archive.domain.collection.Collection
import com.bookcover.archive.domain.collection.CollectionVisibility
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Repository for Collection entity operations.
 *
 * Requirements:
 * - 11.1: Collection creation with name, description, visibility
 * - 11.2: Add book covers to collections
 * - 11.4: Public collections viewable by others
 * - 11.5: Private collections only viewable by owner
 * - 11.9: Share link for public collections
 * - 11.10: Link-only sharing
 */
@Repository
interface CollectionRepository : JpaRepository<Collection, UUID> {

    /**
     * Finds a collection by its share token.
     * Requirement 11.9, 11.10: Share link access
     * @param shareToken The unique share token
     * @return The collection if found
     */
    fun findByShareToken(shareToken: String): Collection?

    /**
     * Finds all collections owned by a user.
     * @param ownerId The owner's UUID
     * @param pageable Pagination information
     * @return Page of collections
     */
    fun findByOwnerId(ownerId: UUID, pageable: Pageable): Page<Collection>

    /**
     * Finds all collections owned by a user (unpaged).
     * @param ownerId The owner's UUID
     * @return List of collections
     */
    fun findByOwnerId(ownerId: UUID): List<Collection>

    /**
     * Finds all public collections.
     * Requirement 11.4: Public collections viewable by others
     * @param visibility The visibility setting
     * @param pageable Pagination information
     * @return Page of public collections
     */
    fun findByVisibility(visibility: CollectionVisibility, pageable: Pageable): Page<Collection>

    /**
     * Finds collections by owner and visibility.
     * @param ownerId The owner's UUID
     * @param visibility The visibility setting
     * @param pageable Pagination information
     * @return Page of collections
     */
    fun findByOwnerIdAndVisibility(
        ownerId: UUID,
        visibility: CollectionVisibility,
        pageable: Pageable
    ): Page<Collection>

    /**
     * Counts collections owned by a user.
     * @param ownerId The owner's UUID
     * @return Count of collections
     */
    fun countByOwnerId(ownerId: UUID): Long

    /**
     * Counts collections by visibility.
     * @param visibility The visibility setting
     * @return Count of collections
     */
    fun countByVisibility(visibility: CollectionVisibility): Long

    /**
     * Checks if a collection with the given name exists for a user.
     * @param ownerId The owner's UUID
     * @param name The collection name
     * @return true if a collection with this name exists
     */
    fun existsByOwnerIdAndName(ownerId: UUID, name: String): Boolean

    /**
     * Finds a collection by owner and name.
     * @param ownerId The owner's UUID
     * @param name The collection name
     * @return The collection if found
     */
    fun findByOwnerIdAndName(ownerId: UUID, name: String): Collection?
}
