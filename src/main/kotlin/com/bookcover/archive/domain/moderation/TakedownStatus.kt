package com.bookcover.archive.domain.moderation

/**
 * Status of a copyright takedown request.
 *
 * Requirements:
 * - 17.1: Process copyright takedown requests from rights holders
 */
enum class TakedownStatus {
    /** Request is awaiting review */
    PENDING,

    /** Request was approved and content was taken down */
    APPROVED,

    /** Request was rejected */
    REJECTED,

    /** Request was withdrawn by the requester */
    WITHDRAWN
}
