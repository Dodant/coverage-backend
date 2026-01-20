package com.bookcover.archive.repository.book

import com.bookcover.archive.domain.book.TranslatedBook
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * Custom repository interface for advanced TranslatedBook queries.
 *
 * Requirements:
 * - 12.1: ISBN duplicate detection
 * - 12.2: Similar book detection
 */
interface TranslatedBookCustomRepository {

    /**
     * Finds translated books with similar titles using fuzzy matching.
     * @param title The title to search for
     * @param pageable Pagination information
     * @return Page of similar translated books
     */
    fun findBySimilarTitle(title: String, pageable: Pageable): Page<TranslatedBook>

    /**
     * Finds translated books that might be duplicates based on title, translator, and year.
     * @param title The book title
     * @param translatorName The translator name
     * @param year The publication year
     * @param pageable Pagination information
     * @return Page of potential duplicates
     */
    fun findPotentialDuplicates(
        title: String,
        translatorName: String,
        year: Int,
        pageable: Pageable
    ): Page<TranslatedBook>

    /**
     * Searches translated books by multiple criteria.
     * @param title Optional title search term
     * @param authorName Optional author name
     * @param translatorName Optional translator name
     * @param publisherName Optional publisher name
     * @param countryCode Optional country code
     * @param languageCode Optional language code
     * @param yearFrom Optional start year
     * @param yearTo Optional end year
     * @param pageable Pagination information
     * @return Page of matching translated books
     */
    fun searchBooks(
        title: String?,
        authorName: String?,
        translatorName: String?,
        publisherName: String?,
        countryCode: String?,
        languageCode: String?,
        yearFrom: Int?,
        yearTo: Int?,
        pageable: Pageable
    ): Page<TranslatedBook>
}
