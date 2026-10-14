package com.bookcover.archive.service.impl;

import com.bookcover.archive.config.security.CustomUserDetails;
import com.bookcover.archive.domain.book.OriginalBook;
import com.bookcover.archive.domain.book.Redesign;
import com.bookcover.archive.domain.book.TranslatedBook;
import com.bookcover.archive.domain.common.ContentStatus;
import com.bookcover.archive.domain.report.Report;
import com.bookcover.archive.domain.report.ReportReason;
import com.bookcover.archive.domain.report.ReportStatus;
import com.bookcover.archive.domain.report.ReportTargetType;
import com.bookcover.archive.domain.social.Comment;
import com.bookcover.archive.domain.user.User;
import com.bookcover.archive.domain.user.UserStatus;
import com.bookcover.archive.event.ContentEvent;
import com.bookcover.archive.event.ContentEventType;
import com.bookcover.archive.event.EventPublisher;
import com.bookcover.archive.event.ModerationEvent;
import com.bookcover.archive.event.ModerationEventType;
import com.bookcover.archive.exception.ResourceNotFoundException;
import com.bookcover.archive.exception.ValidationException;
import com.bookcover.archive.repository.CommentRepository;
import com.bookcover.archive.repository.OriginalBookRepository;
import com.bookcover.archive.repository.RedesignRepository;
import com.bookcover.archive.repository.ReportRepository;
import com.bookcover.archive.repository.TranslatedBookRepository;
import com.bookcover.archive.repository.UserRepository;
import com.bookcover.archive.service.ModerationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

/**
 * 콘텐츠 검수 및 신고 관리 서비스 구현체.
 * 신고 접수, 자동 비공개 처리, 콘텐츠 관리 기능을 구현합니다.
 *
 * Requirements: 15.2, 15.3, 15.4, 15.5, 15.6, 15.7
 */
@Service
@Transactional
public class ModerationServiceImpl implements ModerationService {

    private static final int AUTO_HIDE_THRESHOLD = 3;

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final OriginalBookRepository originalBookRepository;
    private final TranslatedBookRepository translatedBookRepository;
    private final RedesignRepository redesignRepository;
    private final CommentRepository commentRepository;
    private final EventPublisher eventPublisher;

    public ModerationServiceImpl(
            ReportRepository reportRepository,
            UserRepository userRepository,
            OriginalBookRepository originalBookRepository,
            TranslatedBookRepository translatedBookRepository,
            RedesignRepository redesignRepository,
            CommentRepository commentRepository,
            EventPublisher eventPublisher) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.originalBookRepository = originalBookRepository;
        this.translatedBookRepository = translatedBookRepository;
        this.redesignRepository = redesignRepository;
        this.commentRepository = commentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Report createReport(UUID reporterId, ReportTargetType targetType, UUID targetId,
                               ReportReason reason, String description) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("User", reporterId));

        // 신고 제한 확인
        if (Boolean.TRUE.equals(reporter.getReportRestricted())) {
            throw new ValidationException("User is restricted from reporting",
                    Map.of("userId", reporterId));
        }

        // 문제 8 해결: 정지된 사용자 검증 추가 (정지 기간이 지났으면 허용 — 로그인·필터와 같은 규칙)
        if (!new CustomUserDetails(reporter).isAccountNonLocked()) {
            throw new ValidationException("Suspended users cannot create reports",
                    Map.of("userId", reporterId, "status", reporter.getStatus().name()));
        }

        // 중복 신고 확인
        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetId(reporterId, targetType, targetId)) {
            throw new ValidationException("User has already reported this content",
                    Map.of("targetType", targetType, "targetId", targetId));
        }

        // 대상 존재 확인
        validateTargetExists(targetType, targetId);

        Report report = new Report();
        report.setReporter(reporter);
        report.setTargetType(targetType);
        report.setTargetId(targetId);
        report.setReason(reason);
        report.setDescription(description);
        report.setStatus(ReportStatus.PENDING);
        report.setCreatedAt(Instant.now());

        Report savedReport = reportRepository.save(report);

        // 신고 이벤트 발행
        eventPublisher.publishModerationEvent(new ModerationEvent(
                ModerationEventType.CONTENT_REPORTED,
                targetType,
                targetId,
                reporterId,
                reason.name()
        ));

        // 자동 비공개 처리 확인 (3회 이상 신고)
        long reportCount = reportRepository.countByTargetTypeAndTargetId(targetType, targetId);
        if (reportCount >= AUTO_HIDE_THRESHOLD) {
            hideContent(targetType, targetId, "Auto-hidden due to multiple reports");

            // 자동 비공개 이벤트 발행
            eventPublisher.publishModerationEvent(new ModerationEvent(
                    ModerationEventType.CONTENT_AUTO_HIDDEN,
                    targetType,
                    targetId,
                    null,
                    "Auto-hidden due to " + reportCount + " reports"
            ));
        }

        return savedReport;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Report> getReportQueue(Pageable pageable) {
        return reportRepository.findByStatusOrderByCreatedAtAsc(ReportStatus.PENDING, pageable);
    }

    @Override
    public Report reviewReport(UUID reportId, UUID reviewerId, ReportStatus status, String note) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", reportId));

        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", reviewerId));

        // 문제 5 해결: 리뷰어가 ADMIN인지 확인
        if (reviewer.getRole() != com.bookcover.archive.domain.user.UserRole.ADMIN) {
            throw new ValidationException("Only administrators can review reports",
                    Map.of("userId", reviewerId, "role", reviewer.getRole().name()));
        }

        report.review(reviewer, status, note);

        return reportRepository.save(report);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Report> getReportsByTarget(ReportTargetType targetType, UUID targetId, Pageable pageable) {
        return reportRepository.findByTargetTypeAndTargetId(targetType, targetId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public long getReportCount(ReportTargetType targetType, UUID targetId) {
        return reportRepository.countByTargetTypeAndTargetId(targetType, targetId);
    }

    @Override
    public void hideContent(ReportTargetType targetType, UUID targetId, String reason) {
        changeStatus(targetType, targetId, ContentStatus.HIDDEN);
    }

    @Override
    public void restoreContent(ReportTargetType targetType, UUID targetId) {
        changeStatus(targetType, targetId, ContentStatus.PUBLISHED);
    }

    @Override
    public void deleteContent(ReportTargetType targetType, UUID targetId, String reason) {
        changeStatus(targetType, targetId, ContentStatus.DELETED);
    }

    /**
     * 콘텐츠 상태를 바꾸고 검색 문서도 다시 색인한다.
     * 검색은 status=PUBLISHED만 노출하므로 재색인만으로 숨김·복구·삭제가 반영된다.
     */
    private void changeStatus(ReportTargetType targetType, UUID targetId, ContentStatus status) {
        Instant now = Instant.now();
        ContentEventType reindex = switch (targetType) {
            case ORIGINAL_BOOK -> {
                OriginalBook book = originalBookRepository.findById(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException("OriginalBook", targetId));
                book.setStatus(status);
                book.setUpdatedAt(now);
                originalBookRepository.save(book);
                yield ContentEventType.ORIGINAL_BOOK_UPDATED;
            }
            case TRANSLATED_BOOK -> {
                TranslatedBook book = translatedBookRepository.findById(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException("TranslatedBook", targetId));
                book.setStatus(status);
                book.setUpdatedAt(now);
                translatedBookRepository.save(book);
                yield ContentEventType.TRANSLATED_BOOK_UPDATED;
            }
            case REDESIGN -> {
                Redesign redesign = redesignRepository.findById(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException("Redesign", targetId));
                redesign.setStatus(status);
                redesign.setUpdatedAt(now);
                redesignRepository.save(redesign);
                yield ContentEventType.REDESIGN_UPDATED;
            }
            case COMMENT -> {
                Comment comment = commentRepository.findById(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException("Comment", targetId));
                comment.setStatus(status);
                comment.setUpdatedAt(now);
                commentRepository.save(comment);
                yield null; // 댓글은 검색 색인 대상이 아님
            }
        };
        if (reindex != null) {
            eventPublisher.publishContentEvent(new ContentEvent(reindex, targetId, null));
        }
    }

    @Override
    public void suspendUser(UUID userId, long durationMinutes, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        user.setStatus(UserStatus.SUSPENDED);
        user.setSuspendedUntil(Instant.now().plus(durationMinutes, ChronoUnit.MINUTES));
        userRepository.save(user);
    }

    @Override
    public void restrictReporting(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        user.setReportRestricted(true);
        userRepository.save(user);
    }

    @Override
    public void unrestrictReporting(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        user.setReportRestricted(false);
        userRepository.save(user);
    }

    private void validateTargetExists(ReportTargetType targetType, UUID targetId) {
        boolean exists = switch (targetType) {
            case ORIGINAL_BOOK -> originalBookRepository.existsById(targetId);
            case TRANSLATED_BOOK -> translatedBookRepository.existsById(targetId);
            case REDESIGN -> redesignRepository.existsById(targetId);
            case COMMENT -> commentRepository.existsById(targetId);
        };

        if (!exists) {
            throw new ResourceNotFoundException(targetType.name(), targetId);
        }
    }
}
