package com.bookcover.archive.service;

import com.bookcover.archive.domain.user.UserRole;

import java.util.Map;

/**
 * 대시보드 서비스 인터페이스.
 * 관리자 대시보드에 필요한 통계 및 요약 정보를 제공합니다.
 *
 * Requirements:
 * - 19.7: 검색 인덱싱 상태를 조회할 수 있게 한다
 * - 19.8: 시스템 오류 로그를 조회할 수 있게 한다
 * - 19.9: 사용자 통계(가입, 활동, 기여)를 조회할 수 있게 한다
 */
public interface DashboardService {

    /**
     * 전체 통계 요약을 조회합니다.
     */
    DashboardStats getStats();

    /**
     * 사용자 통계를 조회합니다.
     */
    UserStats getUserStats();

    /**
     * 콘텐츠 통계를 조회합니다.
     */
    ContentStats getContentStats();

    /**
     * 신고 통계를 조회합니다.
     */
    ReportStats getReportStats();

    /**
     * 저작권 삭제 요청 통계를 조회합니다.
     */
    TakedownStats getTakedownStats();

    /**
     * 대시보드 전체 통계 DTO
     */
    record DashboardStats(
            UserStats userStats,
            ContentStats contentStats,
            ReportStats reportStats,
            TakedownStats takedownStats
    ) {}

    /**
     * 사용자 통계 DTO
     */
    record UserStats(
            long totalUsers,
            long activeUsers,
            long suspendedUsers,
            long newUsersToday,
            long newUsersThisWeek,
            long newUsersThisMonth,
            Map<UserRole, Long> usersByRole,
            long totalContributors
    ) {}

    /**
     * 콘텐츠 통계 DTO
     */
    record ContentStats(
            long totalOriginalBooks,
            long totalTranslatedBooks,
            long totalRedesigns,
            long totalCovers,
            long newContentToday,
            long newContentThisWeek
    ) {}

    /**
     * 신고 통계 DTO
     */
    record ReportStats(
            long pendingReports,
            long approvedReports,
            long rejectedReports,
            long totalReports
    ) {}

    /**
     * 저작권 삭제 요청 통계 DTO
     */
    record TakedownStats(
            long pendingRequests,
            long approvedRequests,
            long rejectedRequests,
            long totalRequests
    ) {}
}
