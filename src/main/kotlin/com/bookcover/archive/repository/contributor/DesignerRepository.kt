package com.bookcover.archive.repository.contributor

import com.bookcover.archive.domain.contributor.Designer
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Repository for Designer entity operations.
 *
 * Requirements:
 * - 10.3: Designer profile and cover list
 * - 10.4: Designer profile page
 * - 19.6: Alias management
 */
@Repository
interface DesignerRepository : JpaRepository<Designer, UUID> {

    /**
     * Finds a designer by name.
     * @param name The designer name
     * @return The designer if found
     */
    fun findByName(name: String): Designer?

    /**
     * Finds a designer by name (case-insensitive).
     * @param name The designer name
     * @return The designer if found
     */
    fun findByNameIgnoreCase(name: String): Designer?

    /**
     * Checks if a designer with the given name exists.
     * @param name The designer name
     * @return true if a designer with this name exists
     */
    fun existsByName(name: String): Boolean

    /**
     * Finds designers by name containing a search term.
     * @param namePart The search term
     * @param pageable Pagination information
     * @return Page of matching designers
     */
    fun findByNameContainingIgnoreCase(namePart: String, pageable: Pageable): Page<Designer>

    /**
     * Finds designers ordered by cover count (most prolific first).
     * @param pageable Pagination information
     * @return Page of designers
     */
    fun findAllByOrderByCoverCountDesc(pageable: Pageable): Page<Designer>

    /**
     * Finds a designer by alias.
     * @param alias The alias to search for
     * @return The designer if found
     */
    @Query("SELECT d FROM Designer d JOIN d.aliases alias WHERE LOWER(alias) = LOWER(:alias)")
    fun findByAlias(@Param("alias") alias: String): Designer?

    /**
     * Finds designers by name or alias.
     * @param searchTerm The name or alias to search for
     * @param pageable Pagination information
     * @return Page of matching designers
     */
    @Query("""
        SELECT DISTINCT d FROM Designer d
        LEFT JOIN d.aliases alias
        WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        OR LOWER(alias) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
    """)
    fun findByNameOrAlias(@Param("searchTerm") searchTerm: String, pageable: Pageable): Page<Designer>
}
