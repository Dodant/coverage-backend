package com.bookcover.archive.service.impl;

import com.bookcover.archive.domain.report.ReportStatus;
import com.bookcover.archive.domain.takedown.TakedownStatus;
import com.bookcover.archive.domain.user.UserRole;
import com.bookcover.archive.domain.user.UserStatus;
import com.bookcover.archive.domain.user.UserTitle;
import com.bookcover.archive.repository.BookCoverRepository;
import com.bookcover.archive.repository.OriginalBookRepository;
import com.bookcover.archive.repository.RedesignRepository;
import com.bookcover.archive.repository.ReportRepository;
import com.bookcover.archive.repository.TakedownRequestRepository;
import com.bookcover.archive.repository.TranslatedBookRepository;
import com.bookcover.archive.repository.UserRepository;
import com.bookcover.archive.service.DashboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * 대시보드 서비스 구현체.
 * 관리자 대시보드에 필요한 통계 및 요약 정보를 제공합니다.
 *
 * Requirements: 19.7, 19.8, 19.9
 */
@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final UserRepository userRepository;
    private final OriginalBookRepository originalBookRepository;
    private final TranslatedBookRepository translatedBookRepository;
    private final RedesignRepository redesignRepository;
    private final BookCoverRepository bookCoverRepository;
    private final ReportRepository reportRepository;
    private final TakedownRequestRepository takedownRequestRepository;

    public DashboardServiceImpl(
            UserRepository userRepository,
            OriginalBookRepository originalBookRepository,
            TranslatedBookRepository translatedBookRepository,
            RedesignRepository redesignRepository,
            BookCoverRepository bookCoverRepository,
            ReportRepository reportRepository,
            TakedownRequestRepository takedownRequestRepository) {
        this.userRepository = userRepository;
        this.originalBookRepository = originalBookRepository;
        this.translatedBookRepository = translatedBookRepository;
        this.redesignRepository = redesignRepository;
        this.bookCoverRepository = bookCoverRepository;
        this.reportRepository = reportRepository;
        this.takedownRequestRepository = takedownRequestRepository;
    }

    @Override
    public DashboardStats getStats() {
        return new DashboardStats(
                getUserStats(),
                getContentStats(),
                getReportStats(),
                getTakedownStats()
        );
    }

    @Override
    public UserStats getUserStats() {
        Instant now = Instant.now();
        Instant startOfToday = now.truncatedTo(ChronoUnit.DAYS);
        Instant startOfWeek = now.minus(7, ChronoUnit.DAYS);
        Instant startOfMonth = now.minus(30, ChronoUnit.DAYS);

        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatus(UserStatus.ACTIVE);
        long suspendedUsers = userRepository.countByStatus(UserStatus.SUSPENDED);

        // 신규 가입자 통계
        long newUsersToday = userRepository.findByCreatedAtAfter(startOfToday, null).getTotalElements();
        long newUsersThisWeek = userRepository.findByCreatedAtAfter(startOfWeek, null).getTotalElements();
        long newUsersThisMonth = userRepository.findByCreatedAtAfter(startOfMonth, null).getTotalElements();

        // 역할별 사용자 수
        Map<UserRole, Long> usersByRole = new HashMap<>();
        for (UserRole role : UserRole.values()) {
            usersByRole.put(role, userRepository.countByRole(role));
        }

        // 컨트리뷰터 수
        long totalContributors = userRepository.findByTitle(UserTitle.CONTRIBUTOR, null).getTotalElements();

        return new UserStats(
                totalUsers,
                activeUsers,
                suspendedUsers,
                newUsersToday,
                newUsersThisWeek,
                newUsersThisMonth,
                usersByRole,
                totalContributors
        );
    }

    @Override
    public ContentStats getContentStats() {
        long totalOriginalBooks = originalBookRepository.count();
        long totalTranslatedBooks = translatedBookRepository.count();
        long totalRedesigns = redesignRepository.count();
        long totalCovers = bookCoverRepository.count();

        // 신규 콘텐츠 통계는 createdAt 필드가 있는 경우에만 계산 가능
        // 현재는 간단히 0으로 설정 (실제 구현 시 쿼리 추가 필요)
        long newContentToday = 0;
        long newContentThisWeek = 0;

        return new ContentStats(
                totalOriginalBooks,
                totalTranslatedBooks,
                totalRedesigns,
                totalCovers,
                newContentToday,
                newContentThisWeek
        );
    }

    @Override
    public ReportStats getReportStats() {
        long pendingReports = reportRepository.countByStatus(ReportStatus.PENDING);
        long approvedReports = reportRepository.countByStatus(ReportStatus.APPROVED);
        long rejectedReports = reportRepository.countByStatus(ReportStatus.REJECTED);
        long totalReports = reportRepository.count();

        return new ReportStats(
                pendingReports,
                approvedReports,
                rejectedReports,
                totalReports
        );
    }

    @Override
    public TakedownStats getTakedownStats() {
        long pendingRequests = takedownRequestRepository.countByStatus(TakedownStatus.PENDING);
        long approvedRequests = takedownRequestRepository.countByStatus(TakedownStatus.APPROVED);
        long rejectedRequests = takedownRequestRepository.countByStatus(TakedownStatus.REJECTED);
        long totalRequests = takedownRequestRepository.count();

        return new TakedownStats(
                pendingRequests,
                approvedRequests,
                rejectedRequests,
                totalRequests
        );
    }
}
