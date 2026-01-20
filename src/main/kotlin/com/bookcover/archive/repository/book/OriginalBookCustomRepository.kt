package com.bookcover.archive.repository.book

import com.bookcover.archive.domain.book.OriginalBook
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * Custom repository interface for advanced OriginalBook queries.
 *
 * Requirements:
 * - 12.1: ISBN duplicate detection
 * - 12.2: Similar book detection by title, author, year
 */
interface OriginalBookCustomRepository {

    /**
     * Finds books with similar titles using fuzzy matching.
     * Requirement 12.2: Similar book detection
     * @param title The title to search for
     * @param pageable Pagination information
     * @return Page of similar books
     */
    fun findBySimilarTitle(title: String, pageable: Pageable): Page<OriginalBook>

    /**
     * Finds books that might be duplicates based on title, author, and year.
     * Requirement 12.2: Similar book detection
     * @param title The book title
     * @param authorName The author name
     * @param year The publication year
     * @param pageable Pagination information
     * @return Page of potential duplicates
     */
    fun findPotentialDuplicates(
        title: String,
        authorName: String,
        year: Int,
        pageable: Pageable
    ): Page<OriginalBook>

    /**
     * Searches books by multiple criteria.
     * @param title Optional title search term
     * @param authorName Optional author name
     * @param publisherName Optional publisher name
     * @param genre Optional genre
     * @param countryCode Optional country code
     * @param yearFrom Optional start year
     * @param yearTo Optional end year
     * @param pageable Pagination information
     * @return Page of matching books
     */
    fun searchBooks(
        title: String?,
        authorName: String?,
        publisherName: String?,
        genre: String?,
        countryCode: String?,
        yearFrom: Int?,
        yearTo: Int?,
        pageable: Pageable
    ): Page<OriginalBook>
}
