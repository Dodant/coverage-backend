package com.bookcover.archive.repository.book

import com.bookcover.archive.domain.book.TranslatedBook
import com.bookcover.archive.domain.common.ContentStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Repository for TranslatedBook entity operations.
 *
 * Requirements:
 * - 2.1: Translated book linked to original book
 * - 2.6: All translations viewable from original book detail page
 */
@Repository
interface TranslatedBookRepository : JpaRepository<TranslatedBook, UUID> {

    /**
     * Finds a translated book by its ISBN.
     * @param isbn The ISBN to search for
     * @return The translated book if found
     */
    fun findByIsbn(isbn: String): TranslatedBook?

    /**
     * Checks if a translated book with the given ISBN exists.
     * @param isbn The ISBN to check
     * @return true if a book with this ISBN exists
     */
    fun existsByIsbn(isbn: String): Boolean

    /**
     * Finds all translations of an original book.
     * Requirement 2.6: All translations viewable from original book
     * @param originalBookId The original book's UUID
     * @param pageable Pagination information
     * @return Page of translated books
     */
    fun findByOriginalBookId(originalBookId: UUID, pageable: Pageable): Page<TranslatedBook>

    /**
     * Finds all translations of an original book (unpaged).
     * @param originalBookId The original book's UUID
     * @return List of all translated books
     */
    fun findByOriginalBookId(originalBookId: UUID): List<TranslatedBook>

    /**
     * Counts translations of an original book.
     * @param originalBookId The original book's UUID
     * @return Count of translations
     */
    fun countByOriginalBookId(originalBookId: UUID): Long

    /**
     * Finds all translated books by author ID.
     * @param authorId The author's UUID
     * @param pageable Pagination information
     * @return Page of translated books by the author
     */
    fun findByAuthorId(authorId: UUID, pageable: Pageable): Page<TranslatedBook>

    /**
     * Finds all translated books by translator ID.
     * @param translatorId The translator's UUID
     * @param pageable Pagination information
     * @return Page of translated books by the translator
     */
    fun findByTranslatorId(translatorId: UUID, pageable: Pageable): Page<TranslatedBook>

    /**
     * Finds all translated books by publisher ID.
     * @param publisherId The publisher's UUID
     * @param pageable Pagination information
     * @return Page of translated books by the publisher
     */
    fun findByPublisherId(publisherId: UUID, pageable: Pageable): Page<TranslatedBook>

    /**
     * Finds all translated books by country code.
     * @param countryCode ISO 3166-1 country code
     * @param pageable Pagination information
     * @return Page of translated books from the country
     */
    fun findByCountryCode(countryCode: String, pageable: Pageable): Page<TranslatedBook>

    /**
     * Finds all translated books by language code.
     * @param languageCode ISO 639-1 language code
     * @param pageable Pagination information
     * @return Page of translated books in the language
     */
    fun findByLanguageCode(languageCode: String, pageable: Pageable): Page<TranslatedBook>

    /**
     * Finds all translated books by publication year.
     * @param year The publication year
     * @param pageable Pagination information
     * @return Page of translated books from the year
     */
    fun findByYear(year: Int, pageable: Pageable): Page<TranslatedBook>

    /**
     * Finds all translated books by status.
     * @param status The content status
     * @param pageable Pagination information
     * @return Page of translated books with the status
     */
    fun findByStatus(status: ContentStatus, pageable: Pageable): Page<TranslatedBook>

    /**
     * Finds all translated books created by a specific user.
     * @param createdById The user's UUID
     * @param pageable Pagination information
     * @return Page of translated books created by the user
     */
    fun findByCreatedById(createdById: UUID, pageable: Pageable): Page<TranslatedBook>

    /**
     * Counts translated books by author ID.
     * @param authorId The author's UUID
     * @return Count of books by the author
     */
    fun countByAuthorId(authorId: UUID): Long

    /**
     * Counts translated books by translator ID.
     * @param translatorId The translator's UUID
     * @return Count of books by the translator
     */
    fun countByTranslatorId(translatorId: UUID): Long

    /**
     * Counts translated books by publisher ID.
     * @param publisherId The publisher's UUID
     * @return Count of books by the publisher
     */
    fun countByPublisherId(publisherId: UUID): Long
}
