package com.bookcover.archive.repository;

import com.bookcover.archive.domain.report.Report;
import com.bookcover.archive.domain.report.ReportReason;
import com.bookcover.archive.domain.report.ReportStatus;
import com.bookcover.archive.domain.report.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 신고 Repository.
 * 신고 엔티티에 대한 데이터 접근을 제공합니다.
 *
 * Requirements:
 * - 15.3: 콘텐츠가 신고되면 신고 사유, 신고자, 신고 시간을 기록한다
 */
@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {

    /**
     * 상태로 신고 목록을 조회합니다.
     *
     * @param status 신고 상태
     * @param pageable 페이징 정보
     * @return 신고 페이지
     */
    Page<Report> findByStatus(ReportStatus status, Pageable pageable);

    /**
     * 대상 유형과 대상 ID로 신고 목록을 조회합니다.
     *
     * @param targetType 대상 유형
     * @param targetId 대상 ID
     * @return 신고 목록
     */
    List<Report> findByTargetTypeAndTargetId(ReportTargetType targetType, UUID targetId);

    /**
     * 대상 유형과 대상 ID로 신고 목록을 페이징하여 조회합니다.
     *
     * @param targetType 대상 유형
     * @param targetId 대상 ID
     * @param pageable 페이징 정보
     * @return 신고 페이지
     */
    Page<Report> findByTargetTypeAndTargetId(ReportTargetType targetType, UUID targetId, Pageable pageable);

    /**
     * 대상 유형, 대상 ID, 상태로 신고 수를 조회합니다.
     *
     * @param targetType 대상 유형
     * @param targetId 대상 ID
     * @param status 신고 상태
     * @return 신고 수
     */
    long countByTargetTypeAndTargetIdAndStatus(ReportTargetType targetType, UUID targetId, ReportStatus status);

    /**
     * 대상 유형과 대상 ID로 신고 수를 조회합니다.
     *
     * @param targetType 대상 유형
     * @param targetId 대상 ID
     * @return 신고 수
     */
    long countByTargetTypeAndTargetId(ReportTargetType targetType, UUID targetId);

    /**
     * 신고자 ID로 신고 목록을 조회합니다.
     *
     * @param reporterId 신고자 ID
     * @param pageable 페이징 정보
     * @return 신고 페이지
     */
    Page<Report> findByReporterId(UUID reporterId, Pageable pageable);

    /**
     * 신고자 ID로 신고 수를 조회합니다.
     *
     * @param reporterId 신고자 ID
     * @return 신고 수
     */
    long countByReporterId(UUID reporterId);

    /**
     * 특정 기간 내에 신고자가 제출한 신고 수를 조회합니다 (레이트 리밋용).
     *
     * @param reporterId 신고자 ID
     * @param after 시작 시간
     * @return 신고 수
     */
    long countByReporterIdAndCreatedAtAfter(UUID reporterId, Instant after);

    /**
     * 신고 사유로 신고 목록을 조회합니다.
     *
     * @param reason 신고 사유
     * @param pageable 페이징 정보
     * @return 신고 페이지
     */
    Page<Report> findByReason(ReportReason reason, Pageable pageable);

    /**
     * 검토자 ID로 신고 목록을 조회합니다.
     *
     * @param reviewedById 검토자 ID
     * @param pageable 페이징 정보
     * @return 신고 페이지
     */
    Page<Report> findByReviewedById(UUID reviewedById, Pageable pageable);

    /**
     * 상태별 신고 수를 조회합니다.
     *
     * @param status 신고 상태
     * @return 신고 수
     */
    long countByStatus(ReportStatus status);

    /**
     * 대상 유형별 신고 수를 조회합니다.
     *
     * @param targetType 대상 유형
     * @return 신고 수
     */
    long countByTargetType(ReportTargetType targetType);

    /**
     * 대기 중인 신고 목록을 생성일 기준 오름차순으로 조회합니다 (신고 큐).
     *
     * @param status 신고 상태
     * @param pageable 페이징 정보
     * @return 신고 페이지
     */
    Page<Report> findByStatusOrderByCreatedAtAsc(ReportStatus status, Pageable pageable);

    /**
     * 신고자가 특정 대상에 이미 신고했는지 확인합니다.
     *
     * @param reporterId 신고자 ID
     * @param targetType 대상 유형
     * @param targetId 대상 ID
     * @return 신고 여부
     */
    boolean existsByReporterIdAndTargetTypeAndTargetId(UUID reporterId, ReportTargetType targetType, UUID targetId);
}
