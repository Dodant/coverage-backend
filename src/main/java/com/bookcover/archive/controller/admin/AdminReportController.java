package com.bookcover.archive.controller.admin;

import com.bookcover.archive.config.security.CustomUserDetails;
import com.bookcover.archive.controller.dto.admin.ReportResponse;
import com.bookcover.archive.controller.dto.admin.ReviewReportRequest;
import com.bookcover.archive.domain.report.Report;
import com.bookcover.archive.domain.report.ReportTargetType;
import com.bookcover.archive.service.ModerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 관리자 신고 관리 API 컨트롤러.
 * 신고 큐 조회 및 처리 엔드포인트를 제공합니다.
 *
 * Requirements: 19.2 - 대시보드에서 신고 큐를 조회하고 처리할 수 있게 한다
 */
@RestController
@RequestMapping("/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Reports", description = "관리자 신고 관리 API")
@SecurityRequirement(name = "bearerAuth")
public class AdminReportController {

    private final ModerationService moderationService;

    public AdminReportController(ModerationService moderationService) {
        this.moderationService = moderationService;
    }

    /**
     * 대기 중인 신고 큐를 조회합니다.
     */
    @GetMapping("/queue")
    @Operation(summary = "신고 큐 조회", description = "대기 중인 신고 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<Page<ReportResponse>> getReportQueue(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Report> reports = moderationService.getReportQueue(pageable);
        Page<ReportResponse> response = reports.map(ReportResponse::from);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 대상에 대한 신고 목록을 조회합니다.
     */
    @GetMapping("/target/{targetType}/{targetId}")
    @Operation(summary = "대상별 신고 조회", description = "특정 대상에 대한 신고 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<Page<ReportResponse>> getReportsByTarget(
            @PathVariable ReportTargetType targetType,
            @PathVariable UUID targetId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Report> reports = moderationService.getReportsByTarget(targetType, targetId, pageable);
        Page<ReportResponse> response = reports.map(ReportResponse::from);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 대상에 대한 신고 수를 조회합니다.
     */
    @GetMapping("/target/{targetType}/{targetId}/count")
    @Operation(summary = "대상별 신고 수 조회", description = "특정 대상에 대한 신고 수를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<Long> getReportCount(
            @PathVariable ReportTargetType targetType,
            @PathVariable UUID targetId) {
        long count = moderationService.getReportCount(targetType, targetId);
        return ResponseEntity.ok(count);
    }

    /**
     * 신고를 검토 처리합니다.
     */
    @PostMapping("/{reportId}/review")
    @Operation(summary = "신고 검토", description = "신고를 검토하고 처리합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "신고 없음")
    })
    public ResponseEntity<ReportResponse> reviewReport(
            @PathVariable UUID reportId,
            @Valid @RequestBody ReviewReportRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Report report = moderationService.reviewReport(
                reportId,
                userDetails.getId(),
                request.status(),
                request.note()
        );
        return ResponseEntity.ok(ReportResponse.from(report));
    }
}
