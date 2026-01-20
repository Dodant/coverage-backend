package com.bookcover.archive.controller.admin;

import com.bookcover.archive.controller.dto.admin.AuditLogResponse;
import com.bookcover.archive.domain.audit.AuditAction;
import com.bookcover.archive.domain.audit.AuditEntityType;
import com.bookcover.archive.domain.audit.AuditLog;
import com.bookcover.archive.service.AuditService;
import com.bookcover.archive.service.DashboardService;
import com.bookcover.archive.service.DashboardService.ContentStats;
import com.bookcover.archive.service.DashboardService.DashboardStats;
import com.bookcover.archive.service.DashboardService.ReportStats;
import com.bookcover.archive.service.DashboardService.TakedownStats;
import com.bookcover.archive.service.DashboardService.UserStats;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/**
 * 관리자 대시보드 API 컨트롤러.
 * 통계 및 로그 조회 엔드포인트를 제공합니다.
 *
 * Requirements: 19.7 - 검색 인덱싱 상태를 조회할 수 있게 한다
 * Requirements: 19.8 - 시스템 오류 로그를 조회할 수 있게 한다
 * Requirements: 19.9 - 사용자 통계(가입, 활동, 기여)를 조회할 수 있게 한다
 */
@RestController
@RequestMapping("/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Dashboard", description = "관리자 대시보드 API")
@SecurityRequirement(name = "bearerAuth")
public class AdminDashboardController {

    private final DashboardService dashboardService;
    private final AuditService auditService;

    public AdminDashboardController(DashboardService dashboardService, AuditService auditService) {
        this.dashboardService = dashboardService;
        this.auditService = auditService;
    }

    /**
     * 전체 대시보드 통계를 조회합니다.
     */
    @GetMapping("/stats")
    @Operation(summary = "전체 통계 조회", description = "대시보드 전체 통계를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<DashboardStats> getStats() {
        DashboardStats stats = dashboardService.getStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * 사용자 통계를 조회합니다.
     */
    @GetMapping("/stats/users")
    @Operation(summary = "사용자 통계 조회", description = "사용자 통계(가입, 활동, 기여)를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<UserStats> getUserStats() {
        UserStats stats = dashboardService.getUserStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * 콘텐츠 통계를 조회합니다.
     */
    @GetMapping("/stats/content")
    @Operation(summary = "콘텐츠 통계 조회", description = "콘텐츠 통계를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<ContentStats> getContentStats() {
        ContentStats stats = dashboardService.getContentStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * 신고 통계를 조회합니다.
     */
    @GetMapping("/stats/reports")
    @Operation(summary = "신고 통계 조회", description = "신고 통계를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<ReportStats> getReportStats() {
        ReportStats stats = dashboardService.getReportStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * 저작권 삭제 요청 통계를 조회합니다.
     */
    @GetMapping("/stats/takedowns")
    @Operation(summary = "저작권 삭제 요청 통계 조회", description = "저작권 삭제 요청 통계를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<TakedownStats> getTakedownStats() {
        TakedownStats stats = dashboardService.getTakedownStats();
        return ResponseEntity.ok(stats);
    }

    // 감사 로그 조회

    /**
     * 최근 감사 로그를 조회합니다.
     */
    @GetMapping("/logs")
    @Operation(summary = "최근 로그 조회", description = "최근 감사 로그를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<Page<AuditLogResponse>> getRecentLogs(
            @PageableDefault(size = 50) Pageable pageable) {
        Page<AuditLog> logs = auditService.getRecentLogs(pageable);
        Page<AuditLogResponse> response = logs.map(AuditLogResponse::from);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 기간의 감사 로그를 조회합니다.
     */
    @GetMapping("/logs/range")
    @Operation(summary = "기간별 로그 조회", description = "특정 기간의 감사 로그를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<Page<AuditLogResponse>> getLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end,
            @PageableDefault(size = 50) Pageable pageable) {
        Page<AuditLog> logs = auditService.getLogsByDateRange(start, end, pageable);
        Page<AuditLogResponse> response = logs.map(AuditLogResponse::from);
        return ResponseEntity.ok(response);
    }

    /**
     * 액션 유형별 감사 로그를 조회합니다.
     */
    @GetMapping("/logs/action/{action}")
    @Operation(summary = "액션별 로그 조회", description = "특정 액션 유형의 감사 로그를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<Page<AuditLogResponse>> getLogsByAction(
            @PathVariable AuditAction action,
            @PageableDefault(size = 50) Pageable pageable) {
        Page<AuditLog> logs = auditService.getLogsByAction(action, pageable);
        Page<AuditLogResponse> response = logs.map(AuditLogResponse::from);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 엔티티의 수정 이력을 조회합니다.
     */
    @GetMapping("/logs/entity/{entityType}/{entityId}")
    @Operation(summary = "엔티티 이력 조회", description = "특정 엔티티의 수정 이력을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<Page<AuditLogResponse>> getEntityHistory(
            @PathVariable AuditEntityType entityType,
            @PathVariable UUID entityId,
            @PageableDefault(size = 50) Pageable pageable) {
        Page<AuditLog> logs = auditService.getEntityHistory(entityType, entityId, pageable);
        Page<AuditLogResponse> response = logs.map(AuditLogResponse::from);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 사용자가 수행한 작업 이력을 조회합니다.
     */
    @GetMapping("/logs/user/{userId}")
    @Operation(summary = "사용자 작업 이력 조회", description = "특정 사용자가 수행한 작업 이력을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<Page<AuditLogResponse>> getUserActions(
            @PathVariable UUID userId,
            @PageableDefault(size = 50) Pageable pageable) {
        Page<AuditLog> logs = auditService.getUserActions(userId, pageable);
        Page<AuditLogResponse> response = logs.map(AuditLogResponse::from);
        return ResponseEntity.ok(response);
    }
}
