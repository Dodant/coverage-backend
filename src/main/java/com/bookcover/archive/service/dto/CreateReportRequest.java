package com.bookcover.archive.service.dto;

import com.bookcover.archive.domain.report.ReportReason;
import com.bookcover.archive.domain.report.ReportTargetType;

import java.util.UUID;

/**
 * 신고 생성 요청 DTO.
 *
 * @param reporterId 신고자 ID
 * @param targetType 대상 유형
 * @param targetId 대상 ID
 * @param reason 신고 사유
 * @param description 상세 설명 (선택)
 */
public record CreateReportRequest(
        UUID reporterId,
        ReportTargetType targetType,
        UUID targetId,
        ReportReason reason,
        String description
) {}
