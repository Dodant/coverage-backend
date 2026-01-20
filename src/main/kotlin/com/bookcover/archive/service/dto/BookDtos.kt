package com.bookcover.archive.service.dto

import com.bookcover.archive.domain.book.CoverType
import com.bookcover.archive.domain.book.ImageSource
import java.util.UUID

/**
 * Request DTO for creating an original book.
 * Requirements: 1.1, 1.5, 1.6, 1.7
 */
data class CreateOriginalBookRequest(
    val title: String,
    val originalTitle: String,
    val countryCode: String,
    val languageCode: String,
    val year: Int,
    val genre: String,
    val isbn: String? = null,
    val authorId: UUID? = null,
    val authorName: String? = null,
    val publisherId: UUID? = null,
    val publisherName: String? = null,
    val designerId: UUID? = null,
    val designerName: String? = null,
    val edition: String? = null,
    val workId: UUID? = null,
    val frontCover: CreateBookCoverRequest,
    val backCover: CreateBookCoverRequest? = null
)

/**
 * Request DTO for updating an original book.
 */
data class UpdateOriginalBookRequest(
    val title: String? = null,
    val originalTitle: String? = null,
    val genre: String? = null,
    val isbn: String? = null,
    val authorId: UUID? = null,
    val publisherId: UUID? = null,
    val designerId: UUID? = null,
    val edition: String? = null,
    val workId: UUID? = null
)

/**
 * Request DTO for creating a translated book.
 * Requirements: 2.1, 2.2, 2.5
 */
data class CreateTranslatedBookRequest(
    val title: String,
    val countryCode: String,
    val languageCode: String,
    val year: Int,
    val isbn: String? = null,
    val originalBookId: UUID,
    val authorId: UUID? = null,
    val translatorId: UUID? = null,
    val translatorName: String? = null,
    val publisherId: UUID? = null,
    val publisherName: String? = null,
    val edition: String? = null,
    val frontCover: CreateBookCoverRequest,
    val backCover: CreateBookCoverRequest? = null
)

/**
 * Request DTO for updating a translated book.
 */
data class UpdateTranslatedBookRequest(
    val title: String? = null,
    val isbn: String? = null,
    val translatorId: UUID? = null,
    val publisherId: UUID? = null,
    val edition: String? = null
)

/**
 * Request DTO for creating a book cover.
 */
data class CreateBookCoverRequest(
    val imageUrl: String,
    val thumbnailUrl: String,
    val originalFileName: String,
    val width: Int,
    val height: Int,
    val fileSize: Long,
    val coverType: CoverType,
    val imageSource: ImageSource? = null,
    val sourceUrl: String? = null,
    val dominantColor: String? = null
)

/**
 * Response DTO for duplicate detection.
 * Requirements: 12.1, 12.2
 */
data class DuplicateCheckResult(
    val hasDuplicate: Boolean,
    val duplicateType: DuplicateType? = null,
    val existingBookId: UUID? = null,
    val existingBookTitle: String? = null,
    val existingBookType: String? = null,
    val similarBooks: List<SimilarBookInfo> = emptyList()
)

enum class DuplicateType {
    EXACT_ISBN_MATCH,
    SIMILAR_TITLE_AUTHOR_YEAR
}

data class SimilarBookInfo(
    val id: UUID,
    val title: String,
    val authorName: String,
    val year: Int,
    val type: String
)
