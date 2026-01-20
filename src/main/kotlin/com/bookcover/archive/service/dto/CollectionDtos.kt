package com.bookcover.archive.service.dto

import com.bookcover.archive.domain.collection.CollectionVisibility
import java.util.UUID

/**
 * Request DTO for creating a collection.
 * Requirement: 11.1
 */
data class CreateCollectionRequest(
    val name: String,
    val description: String? = null,
    val visibility: CollectionVisibility = CollectionVisibility.PRIVATE,
    val ownerId: UUID
)

/**
 * Request DTO for updating a collection.
 */
data class UpdateCollectionRequest(
    val name: String? = null,
    val description: String? = null,
    val visibility: CollectionVisibility? = null
)
