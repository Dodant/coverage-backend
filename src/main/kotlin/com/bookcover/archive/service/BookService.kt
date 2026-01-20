package com.bookcover.archive.service

import com.bookcover.archive.domain.book.OriginalBook
import com.bookcover.archive.domain.book.TranslatedBook
import com.bookcover.archive.domain.media.Media
import com.bookcover.archive.service.dto.CreateOriginalBookRequest
import com.bookcover.archive.service.dto.CreateTranslatedBookRequest
import com.bookcover.archive.service.dto.DuplicateCheckResult
import com.bookcover.archive.service.dto.UpdateOriginalBookRequest
import com.bookcover.archive.service.dto.UpdateTranslatedBookRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

/**
 * Service interface for book management operations.
 *
 * Requirements:
 * - 1.1, 1.5: Original book CRUD operations
 * - 2.1: Translated book CRUD operations
 * - 12.1, 12.4: Duplicate detection and handling
 */
interface BookService {

    // ==================== Original Book Operations ====================

    /**
     * Creates a new original book.
     * Requirements: 1.1, 1.5
     *
     * @param request The creation request containing book details
     * @param createdById The ID of the user creating the book
     * @return The created original book
     * @throws ValidationException if required fields are missing or invalid
     * @throws DuplicateIsbnException if ISBN already exists
     */
    fun createOriginalBook(request: CreateOriginalBookRequest, createdById: UUID): OriginalBook

    /**
     * Updates an existing original book.
     *
     * @param id The ID of the book to update
     * @param request The update request
     * @return The updated original book
     * @throws ResourceNotFoundException if book not found
     */
    fun updateOriginalBook(id: UUID, request: UpdateOriginalBookRequest): OriginalBook

    /**
     * Gets an original book by ID.
     *
     * @param id The book ID
     * @return The original book or null if not found
     */
    fun getOriginalBook(id: UUID): OriginalBook?

    /**
     * Deletes an original book (soft delete - sets status to DELETED).
     *
     * @param id The ID of the book to delete
     * @throws ResourceNotFoundException if book not found
     */
    fun deleteOriginalBook(id: UUID)

    // ==================== Translated Book Operations ====================

    /**
     * Creates a new translated book linked to an original book.
     * Requirements: 2.1
     *
     * @param request The creation request containing book details
     * @param createdById The ID of the user creating the book
     * @return The created translated book
     * @throws ValidationException if required fields are missing or invalid
     * @throws ResourceNotFoundException if original book not found
     * @throws DuplicateIsbnException if ISBN already exists
     */
    fun createTranslatedBook(request: CreateTranslatedBookRequest, createdById: UUID): TranslatedBook

    /**
     * Updates an existing translated book.
     *
     * @param id The ID of the book to update
     * @param request The update request
     * @return The updated translated book
     * @throws ResourceNotFoundException if book not found
     */
    fun updateTranslatedBook(id: UUID, request: UpdateTranslatedBookRequest): TranslatedBook

    /**
     * Gets a translated book by ID.
     *
     * @param id The book ID
     * @return The translated book or null if not found
     */
    fun getTranslatedBook(id: UUID): TranslatedBook?

    /**
     * Deletes a translated book (soft delete - sets status to DELETED).
     *
     * @param id The ID of the book to delete
     * @throws ResourceNotFoundException if book not found
     */
    fun deleteTranslatedBook(id: UUID)

    // ==================== Query Operations ====================

    /**
     * Gets all translations of an original book.
     * Requirements: 2.6
     *
     * @param originalBookId The original book ID
     * @return List of translated books
     */
    fun getTranslationsByOriginalBook(originalBookId: UUID): List<TranslatedBook>

    /**
     * Gets all books by an author.
     *
     * @param authorId The author ID
     * @param pageable Pagination information
     * @return Page of books (original and translated)
     */
    fun getBooksByAuthor(authorId: UUID, pageable: Pageable): Page<Media>

    /**
     * Gets all books by a publisher.
     *
     * @param publisherId The publisher ID
     * @param pageable Pagination information
     * @return Page of books (original and translated)
     */
    fun getBooksByPublisher(publisherId: UUID, pageable: Pageable): Page<Media>

    /**
     * Gets all books by a designer.
     *
     * @param designerId The designer ID
     * @param pageable Pagination information
     * @return Page of original books
     */
    fun getBooksByDesigner(designerId: UUID, pageable: Pageable): Page<OriginalBook>

    // ==================== Duplicate Detection ====================

    /**
     * Checks for duplicate books by ISBN.
     * Requirements: 12.1
     *
     * @param isbn The ISBN to check
     * @return Duplicate check result
     */
    fun checkDuplicateByIsbn(isbn: String): DuplicateCheckResult

    /**
     * Finds similar books by title, author, and year.
     * Requirements: 12.2
     *
     * @param title The book title
     * @param authorName The author name
     * @param year The publication year
     * @return Duplicate check result with similar books
     */
    fun findSimilarBooks(title: String, authorName: String, year: Int): DuplicateCheckResult

    /**
     * Links a book to an existing work (for edition grouping).
     * Requirements: 12.4
     *
     * @param bookId The book ID
     * @param workId The work ID to link to
     * @return The updated book
     */
    fun linkBookToWork(bookId: UUID, workId: UUID): OriginalBook
}
