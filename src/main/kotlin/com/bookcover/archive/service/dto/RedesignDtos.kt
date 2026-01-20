package com.bookcover.archive.service.dto

import java.util.UUID

/**
 * Request DTO for creating a redesign.
 * Requirements: 3.1, 3.2
 */
data class CreateRedesignRequest(
    val imageUrl: String,
    val thumbnailUrl: String,
    val description: String,
    val originalBookId: UUID? = null,
    val translatedBookId: UUID? = null
)

/**
 * Request DTO for updating a redesign.
 */
data class UpdateRedesignRequest(
    val description: String? = null,
    val imageUrl: String? = null,
    val thumbnailUrl: String? = null
)
