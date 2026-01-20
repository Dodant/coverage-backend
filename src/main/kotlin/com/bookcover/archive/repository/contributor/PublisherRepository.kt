package com.bookcover.archive.repository.contributor

import com.bookcover.archive.domain.contributor.Publisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Repository for Publisher entity operations.
 *
 * Requirements:
 * - 10.1: Publisher profile and book list
 * - 10.4: Publisher profile page
 * - 19.6: Alias management
 */
@Repository
interface PublisherRepository : JpaRepository<Publisher, UUID> {

    /**
     * Finds a publisher by name.
     * @param name The publisher name
     * @return The publisher if found
     */
    fun findByName(name: String): Publisher?

    /**
     * Finds a publisher by name (case-insensitive).
     * @param name The publisher name
     * @return The publisher if found
     */
    fun findByNameIgnoreCase(name: String): Publisher?

    /**
     * Checks if a publisher with the given name exists.
     * @param name The publisher name
     * @return true if a publisher with this name exists
     */
    fun existsByName(name: String): Boolean

    /**
     * Finds publishers by name containing a search term.
     * @param namePart The search term
     * @param pageable Pagination information
     * @return Page of matching publishers
     */
    fun findByNameContainingIgnoreCase(namePart: String, pageable: Pageable): Page<Publisher>

    /**
     * Finds publishers by country code.
     * @param countryCode ISO 3166-1 country code
     * @param pageable Pagination information
     * @return Page of publishers from the country
     */
    fun findByCountryCode(countryCode: String, pageable: Pageable): Page<Publisher>

    /**
     * Finds publishers ordered by book count (most prolific first).
     * @param pageable Pagination information
     * @return Page of publishers
     */
    fun findAllByOrderByBookCountDesc(pageable: Pageable): Page<Publisher>

    /**
     * Finds a publisher by alias.
     * @param alias The alias to search for
     * @return The publisher if found
     */
    @Query("SELECT p FROM Publisher p JOIN p.aliases alias WHERE LOWER(alias) = LOWER(:alias)")
    fun findByAlias(@Param("alias") alias: String): Publisher?

    /**
     * Finds publishers by name or alias.
     * @param searchTerm The name or alias to search for
     * @param pageable Pagination information
     * @return Page of matching publishers
     */
    @Query("""
        SELECT DISTINCT p FROM Publisher p
        LEFT JOIN p.aliases alias
        WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        OR LOWER(alias) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
    """)
    fun findByNameOrAlias(@Param("searchTerm") searchTerm: String, pageable: Pageable): Page<Publisher>
}
