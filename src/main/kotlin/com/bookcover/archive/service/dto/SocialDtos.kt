package com.bookcover.archive.service.dto

import com.bookcover.archive.domain.social.CommentTargetType
import java.util.UUID

/**
 * Request DTO for creating a comment.
 * Requirements: 5.1, 5.2
 */
data class CreateCommentRequest(
    val content: String,
    val authorId: UUID,
    val targetType: CommentTargetType,
    val targetId: UUID
)

/**
 * Response DTO for like status.
 */
data class LikeStatusResponse(
    val isLiked: Boolean,
    val likeCount: Int
)
