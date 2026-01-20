package com.bookcover.archive.repository.book

import com.bookcover.archive.domain.book.OriginalBook
import com.bookcover.archive.domain.common.ContentStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Repository for OriginalBook entity operations.
 *
 * Requirements:
 * - 1.5: Unique identifier generation and database storage
 * - 12.1: ISBN duplicate detection
 * - 12.2: Similar book detection by title, author, year
 */
@Repository
interface OriginalBookRepository : JpaRepository<OriginalBook, UUID> {

    /**
     * Finds an original book by its ISBN.
     * @param isbn The ISBN to search for
     * @return The original book if found
     */
    fun findByIsbn(isbn: String): OriginalBook?

    /**
     * Checks if an original book with the given ISBN exists.
     * @param isbn The ISBN to check
     * @return true if a book with this ISBN exists
     */
    fun existsByIsbn(isbn: String): Boolean

    /**
     * Finds all original books by author ID.
     * @param authorId The author's UUID
     * @param pageable Pagination information
     * @return Page of original books by the author
     */
    fun findByAuthorId(authorId: UUID, pageable: Pageable): Page<OriginalBook>

    /**
     * Finds all original books by publisher ID.
     * @param publisherId The publisher's UUID
     * @param pageable Pagination information
     * @return Page of original books by the publisher
     */
    fun findByPublisherId(publisherId: UUID, pageable: Pageable): Page<OriginalBook>

    /**
     * Finds all original books by designer ID.
     * @param designerId The designer's UUID
     * @param pageable Pagination information
     * @return Page of original books by the designer
     */
    fun findByDesignerId(designerId: UUID, pageable: Pageable): Page<OriginalBook>

    /**
     * Finds all original books by work ID (same literary work).
     * @param workId The work's UUID
     * @param pageable Pagination information
     * @return Page of original books belonging to the same work
     */
    fun findByWorkId(workId: UUID, pageable: Pageable): Page<OriginalBook>

    /**
     * Finds all original books by country code.
     * @param countryCode ISO 3166-1 country code
     * @param pageable Pagination information
     * @return Page of original books from the country
     */
    fun findByCountryCode(countryCode: String, pageable: Pageable): Page<OriginalBook>

    /**
     * Finds all original books by publication year.
     * @param year The publication year
     * @param pageable Pagination information
     * @return Page of original books from the year
     */
    fun findByYear(year: Int, pageable: Pageable): Page<OriginalBook>

    /**
     * Finds all original books by genre.
     * @param genre The genre
     * @param pageable Pagination information
     * @return Page of original books in the genre
     */
    fun findByGenre(genre: String, pageable: Pageable): Page<OriginalBook>

    /**
     * Finds all original books by status.
     * @param status The content status
     * @param pageable Pagination information
     * @return Page of original books with the status
     */
    fun findByStatus(status: ContentStatus, pageable: Pageable): Page<OriginalBook>

    /**
     * Finds all original books created by a specific user.
     * @param createdById The user's UUID
     * @param pageable Pagination information
     * @return Page of original books created by the user
     */
    fun findByCreatedById(createdById: UUID, pageable: Pageable): Page<OriginalBook>

    /**
     * Counts original books by author ID.
     * @param authorId The author's UUID
     * @return Count of books by the author
     */
    fun countByAuthorId(authorId: UUID): Long

    /**
     * Counts original books by publisher ID.
     * @param publisherId The publisher's UUID
     * @return Count of books by the publisher
     */
    fun countByPublisherId(publisherId: UUID): Long

    /**
     * Counts original books by designer ID.
     * @param designerId The designer's UUID
     * @return Count of books by the designer
     */
    fun countByDesignerId(designerId: UUID): Long

    /**
     * Counts original books by work ID.
     * @param workId The work's UUID
     * @return Count of editions for the work
     */
    fun countByWorkId(workId: UUID): Long
}
