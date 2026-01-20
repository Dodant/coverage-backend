package com.bookcover.archive.exception

import java.util.UUID

/**
 * Base exception for Book Cover Archive application.
 */
open class BookArchiveException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * Exception thrown when validation fails.
 */
class ValidationException(
    message: String,
    val errors: Map<String, String> = emptyMap()
) : BookArchiveException(message)

/**
 * Exception thrown when a duplicate ISBN is detected.
 * Requirements: 12.1
 */
class DuplicateIsbnException(
    val isbn: String,
    val existingBookId: UUID,
    val existingBookType: String
) : BookArchiveException("ISBN '$isbn' already exists in the system")

/**
 * Exception thrown when a resource is not found.
 */
class ResourceNotFoundException(
    val resourceType: String,
    val resourceId: UUID
) : BookArchiveException("$resourceType with ID '$resourceId' not found")

/**
 * Exception thrown when an entity is not found by name.
 */
class EntityNotFoundException(
    val entityType: String,
    val identifier: String
) : BookArchiveException("$entityType '$identifier' not found")

/**
 * Exception thrown when a user is not authorized to perform an action.
 */
class UnauthorizedException(
    message: String
) : BookArchiveException(message)

/**
 * Exception thrown when content is hidden or deleted.
 */
class ContentNotAvailableException(
    val contentType: String,
    val contentId: UUID,
    val reason: String
) : BookArchiveException("$contentType with ID '$contentId' is not available: $reason")
