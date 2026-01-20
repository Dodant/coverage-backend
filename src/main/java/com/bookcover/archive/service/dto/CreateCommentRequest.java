package com.bookcover.archive.service.dto;

import com.bookcover.archive.domain.social.CommentTargetType;

import java.util.UUID;

/**
 * 댓글 생성 요청 DTO.
 *
 * @param authorId 작성자 ID
 * @param targetType 대상 유형
 * @param targetId 대상 ID
 * @param content 댓글 내용
 */
public record CreateCommentRequest(
    UUID authorId,
    CommentTargetType targetType,
    UUID targetId,
    String content
) {}
