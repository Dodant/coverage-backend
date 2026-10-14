package com.bookcover.archive.controller;

import com.bookcover.archive.config.ratelimit.RateLimited;
import com.bookcover.archive.config.security.CustomUserDetails;
import com.bookcover.archive.domain.social.LikeTargetType;
import com.bookcover.archive.service.RateLimitAction;
import com.bookcover.archive.service.SocialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 좋아요 API 컨트롤러.
 * 좋아요 토글 및 조회 엔드포인트를 제공합니다.
 *
 * Requirements: 4.1, 4.2
 */
@RestController
@RequestMapping("/likes")
@Tag(name = "Likes", description = "좋아요 관련 API")
public class LikeController {

    private final SocialService socialService;

    public LikeController(SocialService socialService) {
        this.socialService = socialService;
    }

    /**
     * 좋아요를 토글합니다.
     * 좋아요가 없으면 추가하고, 있으면 제거합니다.
     *
     * Requirements: 4.1 - 회원이 북커버에 좋아요를 누르면 해당 회원의 좋아요를 기록하고 좋아요 수를 증가시킨다
     * Requirements: 4.2 - 회원이 이미 좋아요를 누른 북커버에 다시 좋아요를 누르면 좋아요를 취소하고 좋아요 수를 감소시킨다
     */
    @PostMapping("/{targetType}/{targetId}")
    @RateLimited(action = RateLimitAction.LIKE)
    @Operation(summary = "좋아요 토글", description = "좋아요를 토글합니다. 좋아요가 없으면 추가하고, 있으면 제거합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "좋아요 토글 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "대상 없음")
    })
    public ResponseEntity<LikeToggleResponse> toggleLike(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable LikeTargetType targetType,
            @PathVariable UUID targetId) {
        boolean liked = socialService.toggleLike(userDetails.getId(), targetType, targetId);
        int likeCount = socialService.getLikeCount(targetType, targetId);
        return ResponseEntity.ok(new LikeToggleResponse(liked, likeCount));
    }

    /**
     * 대상의 좋아요 수를 조회합니다.
     */
    @GetMapping("/{targetType}/{targetId}/count")
    @Operation(summary = "좋아요 수 조회", description = "대상의 좋아요 수를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<LikeCountResponse> getLikeCount(
            @PathVariable LikeTargetType targetType,
            @PathVariable UUID targetId) {
        int count = socialService.getLikeCount(targetType, targetId);
        return ResponseEntity.ok(new LikeCountResponse(count));
    }

    /**
     * 현재 사용자가 대상에 좋아요를 눌렀는지 확인합니다.
     */
    @GetMapping("/{targetType}/{targetId}/status")
    @Operation(summary = "좋아요 상태 조회", description = "현재 사용자가 대상에 좋아요를 눌렀는지 확인합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<LikeStatusResponse> getLikeStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable LikeTargetType targetType,
            @PathVariable UUID targetId) {
        boolean liked = socialService.isLikedByUser(userDetails.getId(), targetType, targetId);
        int count = socialService.getLikeCount(targetType, targetId);
        return ResponseEntity.ok(new LikeStatusResponse(liked, count));
    }

    /**
     * 좋아요 토글 응답 DTO.
     */
    public record LikeToggleResponse(
            boolean liked,
            int likeCount
    ) {}

    /**
     * 좋아요 수 응답 DTO.
     */
    public record LikeCountResponse(
            int count
    ) {}

    /**
     * 좋아요 상태 응답 DTO.
     */
    public record LikeStatusResponse(
            boolean liked,
            int likeCount
    ) {}
}
