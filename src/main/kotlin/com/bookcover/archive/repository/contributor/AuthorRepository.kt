package com.bookcover.archive.repository.contributor

import com.bookcover.archive.domain.contributor.Author
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Repository for Author entity operations.
 *
 * Requirements:
 * - 10.2: Author profile and book list
 * - 10.4: Author profile page
 * - 19.6: Alias management
 */
@Repository
interface AuthorRepository : JpaRepository<Author, UUID> {

    /**
     * Finds an author by name.
     * @param name The author name
     * @return The author if found
     */
    fun findByName(name: String): Author?

    /**
     * Finds an author by name (case-insensitive).
     * @param name The author name
     * @return The author if found
     */
    fun findByNameIgnoreCase(name: String): Author?

    /**
     * Checks if an author with the given name exists.
     * @param name The author name
     * @return true if an author with this name exists
     */
    fun existsByName(name: String): Boolean

    /**
     * Finds authors by name containing a search term.
     * @param namePart The search term
     * @param pageable Pagination information
     * @return Page of matching authors
     */
    fun findByNameContainingIgnoreCase(namePart: String, pageable: Pageable): Page<Author>

    /**
     * Finds authors ordered by book count (most prolific first).
     * @param pageable Pagination information
     * @return Page of authors
     */
    fun findAllByOrderByBookCountDesc(pageable: Pageable): Page<Author>

    /**
     * Finds an author by alias.
     * @param alias The alias to search for
     * @return The author if found
     */
    @Query("SELECT a FROM Author a JOIN a.aliases alias WHERE LOWER(alias) = LOWER(:alias)")
    fun findByAlias(@Param("alias") alias: String): Author?

    /**
     * Finds authors by name or alias.
     * @param searchTerm The name or alias to search for
     * @param pageable Pagination information
     * @return Page of matching authors
     */
    @Query("""
        SELECT DISTINCT a FROM Author a
        LEFT JOIN a.aliases alias
        WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        OR LOWER(alias) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
    """)
    fun findByNameOrAlias(@Param("searchTerm") searchTerm: String, pageable: Pageable): Page<Author>
}
