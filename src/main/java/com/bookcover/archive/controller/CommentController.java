package com.bookcover.archive.controller;

import com.bookcover.archive.config.ratelimit.RateLimited;
import com.bookcover.archive.config.security.CustomUserDetails;
import com.bookcover.archive.domain.social.Comment;
import com.bookcover.archive.domain.social.CommentTargetType;
import com.bookcover.archive.service.RateLimitAction;
import com.bookcover.archive.service.SocialService;
import com.bookcover.archive.service.dto.CreateCommentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/**
 * 댓글 API 컨트롤러.
 * 댓글 CRUD 엔드포인트를 제공합니다.
 *
 * Requirements: 5.1, 5.4
 */
@RestController
@RequestMapping("/comments")
@Tag(name = "Comments", description = "댓글 관련 API")
public class CommentController {

    private final SocialService socialService;

    public CommentController(SocialService socialService) {
        this.socialService = socialService;
    }

    /**
     * 댓글을 작성합니다.
     *
     * Requirements: 5.1 - 회원이 댓글을 작성하면 댓글 내용, 작성자, 작성 시간을 저장한다
     */
    @PostMapping
    @RateLimited(action = RateLimitAction.COMMENT)
    @Operation(summary = "댓글 작성", description = "새 댓글을 작성합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "댓글 작성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (빈 댓글 등)"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "대상 없음")
    })
    public ResponseEntity<CommentResponse> createComment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateCommentApiRequest request) {
        CreateCommentRequest serviceRequest = new CreateCommentRequest(
                userDetails.getId(),
                request.targetType(),
                request.targetId(),
                request.content()
        );
        Comment comment = socialService.createComment(serviceRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommentResponse.from(comment));
    }

    /**
     * 댓글을 수정합니다.
     */
    @PutMapping("/{commentId}")
    @Operation(summary = "댓글 수정", description = "댓글 내용을 수정합니다. 작성자만 수정할 수 있습니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "댓글 수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (빈 댓글 등)"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (작성자가 아님)"),
            @ApiResponse(responseCode = "404", description = "댓글 없음")
    })
    public ResponseEntity<CommentResponse> updateComment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID commentId,
            @Valid @RequestBody UpdateCommentRequest request) {
        Comment comment = socialService.updateComment(commentId, userDetails.getId(), request.content());
        return ResponseEntity.ok(CommentResponse.from(comment));
    }

    /**
     * 댓글을 삭제합니다.
     *
     * Requirements: 5.4 - 회원이 자신의 댓글을 삭제하면 해당 댓글을 데이터베이스에서 제거한다
     */
    @DeleteMapping("/{commentId}")
    @Operation(summary = "댓글 삭제", description = "댓글을 삭제합니다. 작성자만 삭제할 수 있습니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "댓글 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (작성자가 아님)"),
            @ApiResponse(responseCode = "404", description = "댓글 없음")
    })
    public ResponseEntity<Void> deleteComment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID commentId) {
        socialService.deleteComment(commentId, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * 대상의 댓글 목록을 조회합니다.
     */
    @GetMapping("/{targetType}/{targetId}")
    @Operation(summary = "댓글 목록 조회", description = "대상의 댓글 목록을 시간순으로 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<Page<CommentResponse>> getComments(
            @PathVariable CommentTargetType targetType,
            @PathVariable UUID targetId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Comment> comments = socialService.getComments(targetType, targetId, pageable);
        Page<CommentResponse> response = comments.map(CommentResponse::from);
        return ResponseEntity.ok(response);
    }

    /**
     * 댓글 생성 API 요청 DTO.
     */
    public record CreateCommentApiRequest(
            @NotNull(message = "대상 유형은 필수입니다")
            CommentTargetType targetType,

            @NotNull(message = "대상 ID는 필수입니다")
            UUID targetId,

            @NotBlank(message = "댓글 내용은 필수입니다")
            @Size(max = 1000, message = "댓글은 1000자를 초과할 수 없습니다")
            String content
    ) {}

    /**
     * 댓글 수정 요청 DTO.
     */
    public record UpdateCommentRequest(
            @NotBlank(message = "댓글 내용은 필수입니다")
            @Size(max = 1000, message = "댓글은 1000자를 초과할 수 없습니다")
            String content
    ) {}

    /**
     * 댓글 응답 DTO.
     */
    public record CommentResponse(
            UUID id,
            String content,
            UUID authorId,
            String authorName,
            CommentTargetType targetType,
            UUID targetId,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static CommentResponse from(Comment comment) {
            return new CommentResponse(
                    comment.getId(),
                    comment.getContent(),
                    comment.getAuthor().getId(),
                    comment.getAuthor().getDisplayName(),
                    comment.getTargetType(),
                    comment.getTargetId(),
                    comment.getCreatedAt(),
                    comment.getUpdatedAt()
            );
        }
    }
}
