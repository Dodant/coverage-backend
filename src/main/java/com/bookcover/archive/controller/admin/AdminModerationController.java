package com.bookcover.archive.controller.admin;

import com.bookcover.archive.controller.dto.admin.ContentActionRequest;
import com.bookcover.archive.controller.dto.admin.SuspendUserRequest;
import com.bookcover.archive.service.ModerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 관리자 콘텐츠 관리 API 컨트롤러.
 * 콘텐츠 숨김/복원/삭제 및 사용자 정지 엔드포인트를 제공합니다.
 *
 * Requirements: 15.5 - 관리자가 신고된 콘텐츠를 검토하고 결정을 내릴 수 있게 한다
 * Requirements: 18.6 - 반복적인 어뷰징 행위를 하는 계정을 일시 정지할 수 있게 한다
 */
@RestController
@RequestMapping("/admin/moderation")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Moderation", description = "관리자 콘텐츠 관리 API")
@SecurityRequirement(name = "bearerAuth")
public class AdminModerationController {

    private final ModerationService moderationService;

    public AdminModerationController(ModerationService moderationService) {
        this.moderationService = moderationService;
    }

    /**
     * 콘텐츠를 숨김 처리합니다.
     */
    @PostMapping("/content/hide")
    @Operation(summary = "콘텐츠 숨김", description = "콘텐츠를 숨김 처리합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "콘텐츠 없음")
    })
    public ResponseEntity<Void> hideContent(@Valid @RequestBody ContentActionRequest request) {
        moderationService.hideContent(request.targetType(), request.targetId(), request.reason());
        return ResponseEntity.ok().build();
    }

    /**
     * 숨김 처리된 콘텐츠를 복원합니다.
     */
    @PostMapping("/content/restore")
    @Operation(summary = "콘텐츠 복원", description = "숨김 처리된 콘텐츠를 복원합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "콘텐츠 없음")
    })
    public ResponseEntity<Void> restoreContent(@Valid @RequestBody ContentActionRequest request) {
        moderationService.restoreContent(request.targetType(), request.targetId());
        return ResponseEntity.ok().build();
    }

    /**
     * 콘텐츠를 삭제 처리합니다.
     */
    @PostMapping("/content/delete")
    @Operation(summary = "콘텐츠 삭제", description = "콘텐츠를 삭제 처리합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "콘텐츠 없음")
    })
    public ResponseEntity<Void> deleteContent(@Valid @RequestBody ContentActionRequest request) {
        moderationService.deleteContent(request.targetType(), request.targetId(), request.reason());
        return ResponseEntity.ok().build();
    }

    /**
     * 사용자를 일시 정지합니다.
     */
    @PostMapping("/users/{userId}/suspend")
    @Operation(summary = "사용자 정지", description = "사용자를 일시 정지합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "사용자 없음")
    })
    public ResponseEntity<Void> suspendUser(
            @PathVariable UUID userId,
            @Valid @RequestBody SuspendUserRequest request) {
        moderationService.suspendUser(userId, request.durationMinutes(), request.reason());
        return ResponseEntity.ok().build();
    }

    /**
     * 사용자의 신고 기능을 제한합니다.
     */
    @PostMapping("/users/{userId}/restrict-reporting")
    @Operation(summary = "신고 기능 제한", description = "사용자의 신고 기능을 제한합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "사용자 없음")
    })
    public ResponseEntity<Void> restrictReporting(@PathVariable UUID userId) {
        moderationService.restrictReporting(userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 사용자의 신고 기능 제한을 해제합니다.
     */
    @PostMapping("/users/{userId}/unrestrict-reporting")
    @Operation(summary = "신고 기능 제한 해제", description = "사용자의 신고 기능 제한을 해제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "사용자 없음")
    })
    public ResponseEntity<Void> unrestrictReporting(@PathVariable UUID userId) {
        moderationService.unrestrictReporting(userId);
        return ResponseEntity.ok().build();
    }
}
