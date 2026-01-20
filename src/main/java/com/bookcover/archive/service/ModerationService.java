package com.bookcover.archive.service;

import com.bookcover.archive.domain.report.Report;
import com.bookcover.archive.domain.report.ReportReason;
import com.bookcover.archive.domain.report.ReportStatus;
import com.bookcover.archive.domain.report.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * 콘텐츠 검수 및 신고 관리 서비스 인터페이스.
 * 신고 접수, 자동 비공개 처리, 콘텐츠 관리 기능을 제공합니다.
 *
 * Requirements: 15.2, 15.3, 15.4, 15.5, 15.6, 15.7
 */
public interface ModerationService {

    // 신고 관리

    /**
     * 신고를 접수합니다.
     * 동일 콘텐츠에 대해 3회 이상 신고가 접수되면 자동으로 비공개 처리합니다.
     *
     * @param reporterId 신고자 ID
     * @param targetType 대상 유형
     * @param targetId 대상 ID
     * @param reason 신고 사유
     * @param description 상세 설명
     * @return 생성된 신고
     */
    Report createReport(UUID reporterId, ReportTargetType targetType, UUID targetId,
                        ReportReason reason, String description);

    /**
     * 대기 중인 신고 큐를 조회합니다.
     *
     * @param pageable 페이징 정보
     * @return 신고 페이지
     */
    Page<Report> getReportQueue(Pageable pageable);

    /**
     * 신고를 검토 처리합니다.
     *
     * @param reportId 신고 ID
     * @param reviewerId 검토자 ID
     * @param status 처리 상태
     * @param note 검토 메모
     * @return 처리된 신고
     */
    Report reviewReport(UUID reportId, UUID reviewerId, ReportStatus status, String note);

    /**
     * 특정 대상에 대한 신고 목록을 조회합니다.
     *
     * @param targetType 대상 유형
     * @param targetId 대상 ID
     * @param pageable 페이징 정보
     * @return 신고 페이지
     */
    Page<Report> getReportsByTarget(ReportTargetType targetType, UUID targetId, Pageable pageable);

    /**
     * 특정 대상에 대한 신고 수를 조회합니다.
     *
     * @param targetType 대상 유형
     * @param targetId 대상 ID
     * @return 신고 수
     */
    long getReportCount(ReportTargetType targetType, UUID targetId);

    // 콘텐츠 관리

    /**
     * 콘텐츠를 숨김 처리합니다.
     *
     * @param targetType 대상 유형
     * @param targetId 대상 ID
     * @param reason 숨김 사유
     */
    void hideContent(ReportTargetType targetType, UUID targetId, String reason);

    /**
     * 숨김 처리된 콘텐츠를 복원합니다.
     *
     * @param targetType 대상 유형
     * @param targetId 대상 ID
     */
    void restoreContent(ReportTargetType targetType, UUID targetId);

    /**
     * 콘텐츠를 삭제 처리합니다.
     *
     * @param targetType 대상 유형
     * @param targetId 대상 ID
     * @param reason 삭제 사유
     */
    void deleteContent(ReportTargetType targetType, UUID targetId, String reason);

    // 사용자 관리

    /**
     * 사용자를 일시 정지합니다.
     *
     * @param userId 사용자 ID
     * @param durationMinutes 정지 기간 (분)
     * @param reason 정지 사유
     */
    void suspendUser(UUID userId, long durationMinutes, String reason);

    /**
     * 사용자의 신고 기능을 제한합니다.
     *
     * @param userId 사용자 ID
     */
    void restrictReporting(UUID userId);

    /**
     * 사용자의 신고 기능 제한을 해제합니다.
     *
     * @param userId 사용자 ID
     */
    void unrestrictReporting(UUID userId);
}
