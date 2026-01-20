package com.bookcover.archive.repository.contributor

import com.bookcover.archive.domain.contributor.Translator
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Repository for Translator entity operations.
 *
 * Requirements:
 * - 19.6: Alias management
 */
@Repository
interface TranslatorRepository : JpaRepository<Translator, UUID> {

    /**
     * Finds a translator by name.
     * @param name The translator name
     * @return The translator if found
     */
    fun findByName(name: String): Translator?

    /**
     * Finds a translator by name (case-insensitive).
     * @param name The translator name
     * @return The translator if found
     */
    fun findByNameIgnoreCase(name: String): Translator?

    /**
     * Checks if a translator with the given name exists.
     * @param name The translator name
     * @return true if a translator with this name exists
     */
    fun existsByName(name: String): Boolean

    /**
     * Finds translators by name containing a search term.
     * @param namePart The search term
     * @param pageable Pagination information
     * @return Page of matching translators
     */
    fun findByNameContainingIgnoreCase(namePart: String, pageable: Pageable): Page<Translator>

    /**
     * Finds translators ordered by book count (most prolific first).
     * @param pageable Pagination information
     * @return Page of translators
     */
    fun findAllByOrderByBookCountDesc(pageable: Pageable): Page<Translator>

    /**
     * Finds a translator by alias.
     * @param alias The alias to search for
     * @return The translator if found
     */
    @Query("SELECT t FROM Translator t JOIN t.aliases alias WHERE LOWER(alias) = LOWER(:alias)")
    fun findByAlias(@Param("alias") alias: String): Translator?

    /**
     * Finds translators by name or alias.
     * @param searchTerm The name or alias to search for
     * @param pageable Pagination information
     * @return Page of matching translators
     */
    @Query("""
        SELECT DISTINCT t FROM Translator t
        LEFT JOIN t.aliases alias
        WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        OR LOWER(alias) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
    """)
    fun findByNameOrAlias(@Param("searchTerm") searchTerm: String, pageable: Pageable): Page<Translator>
}
