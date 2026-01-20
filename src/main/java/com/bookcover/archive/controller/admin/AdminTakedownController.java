package com.bookcover.archive.controller.admin;

import com.bookcover.archive.config.security.CustomUserDetails;
import com.bookcover.archive.controller.dto.admin.ReviewTakedownRequest;
import com.bookcover.archive.controller.dto.admin.TakedownResponse;
import com.bookcover.archive.domain.takedown.TakedownRequest;
import com.bookcover.archive.domain.takedown.TakedownStatus;
import com.bookcover.archive.exception.ResourceNotFoundException;
import com.bookcover.archive.service.TakedownService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 관리자 저작권 삭제 요청 관리 API 컨트롤러.
 * 저작권 삭제 요청 조회 및 처리 엔드포인트를 제공합니다.
 *
 * Requirements: 19.4 - 대시보드에서 삭제 요청 목록을 조회하고 처리할 수 있게 한다
 */
@RestController
@RequestMapping("/admin/takedowns")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Takedowns", description = "관리자 저작권 삭제 요청 관리 API")
@SecurityRequirement(name = "bearerAuth")
public class AdminTakedownController {

    private final TakedownService takedownService;

    public AdminTakedownController(TakedownService takedownService) {
        this.takedownService = takedownService;
    }

    /**
     * 대기 중인 저작권 삭제 요청 큐를 조회합니다.
     */
    @GetMapping("/queue")
    @Operation(summary = "삭제 요청 큐 조회", description = "대기 중인 저작권 삭제 요청 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<Page<TakedownResponse>> getPendingRequests(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<TakedownRequest> requests = takedownService.getPendingRequests(pageable);
        Page<TakedownResponse> response = requests.map(TakedownResponse::from);
        return ResponseEntity.ok(response);
    }

    /**
     * 상태별 저작권 삭제 요청 목록을 조회합니다.
     */
    @GetMapping
    @Operation(summary = "삭제 요청 목록 조회", description = "상태별 저작권 삭제 요청 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<Page<TakedownResponse>> getRequestsByStatus(
            @RequestParam(required = false) TakedownStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<TakedownRequest> requests;
        if (status != null) {
            requests = takedownService.getRequestsByStatus(status, pageable);
        } else {
            requests = takedownService.getPendingRequests(pageable);
        }
        Page<TakedownResponse> response = requests.map(TakedownResponse::from);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 저작권 삭제 요청을 조회합니다.
     */
    @GetMapping("/{requestId}")
    @Operation(summary = "삭제 요청 상세 조회", description = "특정 저작권 삭제 요청을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "요청 없음")
    })
    public ResponseEntity<TakedownResponse> getRequest(@PathVariable UUID requestId) {
        TakedownRequest request = takedownService.getRequest(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("TakedownRequest", requestId));
        return ResponseEntity.ok(TakedownResponse.from(request));
    }

    /**
     * 저작권 삭제 요청을 승인합니다.
     */
    @PostMapping("/{requestId}/approve")
    @Operation(summary = "삭제 요청 승인", description = "저작권 삭제 요청을 승인합니다. 해당 이미지를 비공개 처리하고 재업로드 방지 플래그를 설정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "승인 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (이미 처리됨)"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "요청 없음")
    })
    public ResponseEntity<TakedownResponse> approveRequest(
            @PathVariable UUID requestId,
            @RequestBody(required = false) ReviewTakedownRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        String note = request != null ? request.note() : null;
        TakedownRequest takedownRequest = takedownService.approveRequest(requestId, userDetails.getId(), note);
        return ResponseEntity.ok(TakedownResponse.from(takedownRequest));
    }

    /**
     * 저작권 삭제 요청을 거부합니다.
     */
    @PostMapping("/{requestId}/reject")
    @Operation(summary = "삭제 요청 거부", description = "저작권 삭제 요청을 거부합니다. 거부 사유를 요청자에게 통지합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "거부 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (이미 처리됨)"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "요청 없음")
    })
    public ResponseEntity<TakedownResponse> rejectRequest(
            @PathVariable UUID requestId,
            @RequestBody(required = false) ReviewTakedownRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        String note = request != null ? request.note() : null;
        TakedownRequest takedownRequest = takedownService.rejectRequest(requestId, userDetails.getId(), note);
        return ResponseEntity.ok(TakedownResponse.from(takedownRequest));
    }

    /**
     * 상태별 저작권 삭제 요청 수를 조회합니다.
     */
    @GetMapping("/count")
    @Operation(summary = "삭제 요청 수 조회", description = "상태별 저작권 삭제 요청 수를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<Long> countByStatus(@RequestParam TakedownStatus status) {
        long count = takedownService.countByStatus(status);
        return ResponseEntity.ok(count);
    }
}
