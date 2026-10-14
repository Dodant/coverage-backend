package com.bookcover.archive.controller;

import com.bookcover.archive.config.ratelimit.RateLimited;
import com.bookcover.archive.config.security.CustomUserDetails;
import com.bookcover.archive.controller.dto.admin.ReportResponse;
import com.bookcover.archive.domain.report.Report;
import com.bookcover.archive.domain.report.ReportReason;
import com.bookcover.archive.domain.report.ReportTargetType;
import com.bookcover.archive.service.ModerationService;
import com.bookcover.archive.service.RateLimitAction;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 회원 신고 접수 API.
 *
 * Requirements: 15.2, 15.3 - 신고 기능 제공, 사유/신고자/시간 기록
 * 동일 콘텐츠 3회 이상 신고 시 자동 비공개는 ModerationService가 처리합니다.
 */
@RestController
@RequestMapping("/reports")
@Tag(name = "Reports", description = "콘텐츠 신고 API")
public class ReportController {

    private final ModerationService moderationService;

    public ReportController(ModerationService moderationService) {
        this.moderationService = moderationService;
    }

    @PostMapping
    @RateLimited(action = RateLimitAction.REPORT)
    @Operation(summary = "콘텐츠 신고", description = "부적절한 콘텐츠를 신고합니다. 같은 콘텐츠는 한 번만 신고할 수 있습니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "신고 접수"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청, 중복 신고, 신고 제한된 회원"),
            @ApiResponse(responseCode = "404", description = "대상 콘텐츠 없음"),
            @ApiResponse(responseCode = "429", description = "신고 횟수 초과")
    })
    public ResponseEntity<ReportResponse> createReport(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateReportRequest request) {
        Report report = moderationService.createReport(
                userDetails.getId(),
                request.targetType(),
                request.targetId(),
                request.reason(),
                request.description()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ReportResponse.from(report));
    }

    public record CreateReportRequest(
            @NotNull(message = "신고 대상 유형은 필수입니다")
            ReportTargetType targetType,

            @NotNull(message = "신고 대상 ID는 필수입니다")
            UUID targetId,

            @NotNull(message = "신고 사유는 필수입니다")
            ReportReason reason,

            @Size(max = 1000, message = "상세 설명은 1000자 이하여야 합니다")
            String description
    ) {}
}
