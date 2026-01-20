package com.bookcover.archive.service.impl

import com.bookcover.archive.domain.book.Redesign
import com.bookcover.archive.domain.common.ContentStatus
import com.bookcover.archive.exception.ResourceNotFoundException
import com.bookcover.archive.exception.UnauthorizedException
import com.bookcover.archive.exception.ValidationException
import com.bookcover.archive.repository.book.OriginalBookRepository
import com.bookcover.archive.repository.book.RedesignRepository
import com.bookcover.archive.repository.book.TranslatedBookRepository
import com.bookcover.archive.service.RedesignService
import com.bookcover.archive.service.dto.CreateRedesignRequest
import com.bookcover.archive.service.dto.UpdateRedesignRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Implementation of RedesignService.
 *
 * Requirements:
 * - 3.1: Redesign linked to original or translated book
 * - 3.2: Redesign image and description required
 * - 3.5: Multiple users can upload redesigns for the same book
 * - 3.6: One user can upload multiple redesigns for the same book
 */
@Service
@Transactional
class RedesignServiceImpl(
    private val redesignRepository: RedesignRepository,
    private val originalBookRepository: OriginalBookRepository,
    private val translatedBookRepository: TranslatedBookRepository,
    private val contributionService: com.bookcover.archive.service.ContributionService
) : RedesignService {

    override fun createRedesign(request: CreateRedesignRequest, createdById: UUID): Redesign {
        validateCreateRedesignRequest(request)

        val originalBook = request.originalBookId?.let { bookId ->
            originalBookRepository.findById(bookId).orElseThrow {
                ResourceNotFoundException("OriginalBook", bookId)
            }
        }

        val translatedBook = request.translatedBookId?.let { bookId ->
            translatedBookRepository.findById(bookId).orElseThrow {
                ResourceNotFoundException("TranslatedBook", bookId)
            }
        }

        val redesign = Redesign(
            imageUrl = request.imageUrl.trim(),
            thumbnailUrl = request.thumbnailUrl.trim(),
            description = request.description.trim(),
            originalBook = originalBook,
            translatedBook = translatedBook,
            createdById = createdById
        )

        val savedRedesign = redesignRepository.save(redesign)

        // Track user contribution for title eligibility (Requirement 7.5, 7.6)
        contributionService.recordContribution(createdById)

        return savedRedesign
    }

    override fun updateRedesign(id: UUID, request: UpdateRedesignRequest, userId: UUID): Redesign {
        val redesign = redesignRepository.findById(id).orElseThrow {
            ResourceNotFoundException("Redesign", id)
        }

        if (redesign.createdById != userId) {
            throw UnauthorizedException("You are not authorized to update this redesign")
        }

        request.description?.let {
            if (it.isBlank()) {
                throw ValidationException("Description cannot be blank", mapOf("description" to "Description is required"))
            }
            redesign.description = it.trim()
        }

        request.imageUrl?.let {
            if (it.isBlank()) {
                throw ValidationException("Image URL cannot be blank", mapOf("imageUrl" to "Image URL is required"))
            }
            redesign.imageUrl = it.trim()
        }

        request.thumbnailUrl?.let {
            if (it.isBlank()) {
                throw ValidationException("Thumbnail URL cannot be blank", mapOf("thumbnailUrl" to "Thumbnail URL is required"))
            }
            redesign.thumbnailUrl = it.trim()
        }

        return redesignRepository.save(redesign)
    }

    @Transactional(readOnly = true)
    override fun getRedesign(id: UUID): Redesign? {
        return redesignRepository.findById(id).orElse(null)
    }

    override fun deleteRedesign(id: UUID, userId: UUID) {
        val redesign = redesignRepository.findById(id).orElseThrow {
            ResourceNotFoundException("Redesign", id)
        }

        if (redesign.createdById != userId) {
            throw UnauthorizedException("You are not authorized to delete this redesign")
        }

        redesign.status = ContentStatus.DELETED
        redesignRepository.save(redesign)
    }

    @Transactional(readOnly = true)
    override fun getRedesignsByOriginalBook(originalBookId: UUID, pageable: Pageable): Page<Redesign> {
        return redesignRepository.findByOriginalBookIdAndStatus(
            originalBookId,
            ContentStatus.PUBLISHED,
            pageable
        )
    }

    @Transactional(readOnly = true)
    override fun getRedesignsByTranslatedBook(translatedBookId: UUID, pageable: Pageable): Page<Redesign> {
        return redesignRepository.findByTranslatedBookIdAndStatus(
            translatedBookId,
            ContentStatus.PUBLISHED,
            pageable
        )
    }

    @Transactional(readOnly = true)
    override fun getRedesignsByUser(userId: UUID, pageable: Pageable): Page<Redesign> {
        return redesignRepository.findByCreatedById(userId, pageable)
    }

    @Transactional(readOnly = true)
    override fun countRedesignsByOriginalBook(originalBookId: UUID): Long {
        return redesignRepository.countByOriginalBookId(originalBookId)
    }

    @Transactional(readOnly = true)
    override fun countRedesignsByTranslatedBook(translatedBookId: UUID): Long {
        return redesignRepository.countByTranslatedBookId(translatedBookId)
    }

    @Transactional(readOnly = true)
    override fun countRedesignsByUser(userId: UUID): Long {
        return redesignRepository.countByCreatedById(userId)
    }

    private fun validateCreateRedesignRequest(request: CreateRedesignRequest) {
        val errors = mutableMapOf<String, String>()

        if (request.imageUrl.isBlank()) {
            errors["imageUrl"] = "Image URL is required"
        }

        if (request.thumbnailUrl.isBlank()) {
            errors["thumbnailUrl"] = "Thumbnail URL is required"
        }

        if (request.description.isBlank()) {
            errors["description"] = "Description is required"
        }

        if (request.originalBookId == null && request.translatedBookId == null) {
            errors["book"] = "Either originalBookId or translatedBookId must be provided"
        }

        if (request.originalBookId != null && request.translatedBookId != null) {
            errors["book"] = "Only one of originalBookId or translatedBookId should be provided"
        }

        if (errors.isNotEmpty()) {
            throw ValidationException("Validation failed for redesign creation", errors)
        }
    }
}
