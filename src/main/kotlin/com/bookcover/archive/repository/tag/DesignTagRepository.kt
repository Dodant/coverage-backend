package com.bookcover.archive.repository.tag

import com.bookcover.archive.domain.tag.DesignTag
import com.bookcover.archive.domain.tag.TagCategory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Repository for DesignTag entity operations.
 *
 * Requirements:
 * - 20.1: Support design attribute tags
 * - 20.4: Admin can manage tag dictionary and approval policy
 * - 20.6: Tag-based browsing
 */
@Repository
interface DesignTagRepository : JpaRepository<DesignTag, UUID> {

    /**
     * Finds a tag by its name.
     * @param name The tag name
     * @return The tag if found
     */
    fun findByName(name: String): DesignTag?

    /**
     * Checks if a tag with the given name exists.
     * @param name The tag name
     * @return true if a tag with this name exists
     */
    fun existsByName(name: String): Boolean

    /**
     * Finds all tags by category.
     * @param category The tag category
     * @param pageable Pagination information
     * @return Page of tags
     */
    fun findByCategory(category: TagCategory, pageable: Pageable): Page<DesignTag>

    /**
     * Finds all tags by category (unpaged).
     * @param category The tag category
     * @return List of tags
     */
    fun findByCategory(category: TagCategory): List<DesignTag>

    /**
     * Finds all approved tags.
     * @param pageable Pagination information
     * @return Page of approved tags
     */
    fun findByApprovedTrue(pageable: Pageable): Page<DesignTag>

    /**
     * Finds all approved tags (unpaged).
     * @return List of approved tags
     */
    fun findByApprovedTrue(): List<DesignTag>

    /**
     * Finds all unapproved tags (pending approval).
     * @param pageable Pagination information
     * @return Page of unapproved tags
     */
    fun findByApprovedFalse(pageable: Pageable): Page<DesignTag>

    /**
     * Finds approved tags by category.
     * @param category The tag category
     * @param pageable Pagination information
     * @return Page of approved tags
     */
    fun findByCategoryAndApprovedTrue(category: TagCategory, pageable: Pageable): Page<DesignTag>

    /**
     * Finds approved tags by category (unpaged).
     * @param category The tag category
     * @return List of approved tags
     */
    fun findByCategoryAndApprovedTrue(category: TagCategory): List<DesignTag>

    /**
     * Finds tags ordered by usage count (most popular first).
     * @param pageable Pagination information
     * @return Page of tags
     */
    fun findByApprovedTrueOrderByUsageCountDesc(pageable: Pageable): Page<DesignTag>

    /**
     * Finds tags by name containing a search term (case-insensitive).
     * @param namePart The search term
     * @param pageable Pagination information
     * @return Page of matching tags
     */
    fun findByNameContainingIgnoreCase(namePart: String, pageable: Pageable): Page<DesignTag>

    /**
     * Counts tags by category.
     * @param category The tag category
     * @return Count of tags
     */
    fun countByCategory(category: TagCategory): Long

    /**
     * Counts approved tags.
     * @return Count of approved tags
     */
    fun countByApprovedTrue(): Long

    /**
     * Counts unapproved tags.
     * @return Count of unapproved tags
     */
    fun countByApprovedFalse(): Long
}
