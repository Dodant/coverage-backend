package com.bookcover.archive.repository.work

import com.bookcover.archive.domain.work.Work
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Repository for Work entity operations.
 *
 * Requirements:
 * - 6.1: Group same works across different editions
 * - 6.4: View all editions of a work grouped together
 */
@Repository
interface WorkRepository : JpaRepository<Work, UUID> {

    /**
     * Finds a work by its original title.
     * @param originalTitle The original title
     * @return The work if found
     */
    fun findByOriginalTitle(originalTitle: String): Work?

    /**
     * Finds a work by its original title (case-insensitive).
     * @param originalTitle The original title
     * @return The work if found
     */
    fun findByOriginalTitleIgnoreCase(originalTitle: String): Work?

    /**
     * Checks if a work with the given original title exists.
     * @param originalTitle The original title
     * @return true if a work with this title exists
     */
    fun existsByOriginalTitle(originalTitle: String): Boolean

    /**
     * Finds works by original title containing a search term.
     * @param titlePart The search term
     * @param pageable Pagination information
     * @return Page of matching works
     */
    fun findByOriginalTitleContainingIgnoreCase(titlePart: String, pageable: Pageable): Page<Work>

    /**
     * Finds works by original language code.
     * @param originalLanguageCode ISO 639-1 language code
     * @param pageable Pagination information
     * @return Page of works in the language
     */
    fun findByOriginalLanguageCode(originalLanguageCode: String, pageable: Pageable): Page<Work>

    /**
     * Finds works by first published year.
     * @param firstPublishedYear The year
     * @param pageable Pagination information
     * @return Page of works from the year
     */
    fun findByFirstPublishedYear(firstPublishedYear: Int, pageable: Pageable): Page<Work>

    /**
     * Finds works ordered by edition count (most editions first).
     * @param pageable Pagination information
     * @return Page of works
     */
    fun findAllByOrderByEditionCountDesc(pageable: Pageable): Page<Work>

    /**
     * Finds works with multiple editions.
     * @param minEditionCount Minimum number of editions
     * @param pageable Pagination information
     * @return Page of works with multiple editions
     */
    fun findByEditionCountGreaterThanEqual(minEditionCount: Int, pageable: Pageable): Page<Work>
}
