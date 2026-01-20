package com.bookcover.archive.service.impl;

import com.bookcover.archive.domain.user.User;
import com.bookcover.archive.domain.user.UserRole;
import com.bookcover.archive.domain.user.UserTitle;
import com.bookcover.archive.exception.ResourceNotFoundException;
import com.bookcover.archive.repository.OriginalBookRepository;
import com.bookcover.archive.repository.RedesignRepository;
import com.bookcover.archive.repository.TranslatedBookRepository;
import com.bookcover.archive.repository.UserRepository;
import com.bookcover.archive.service.ContributionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 기여 관리 서비스 구현체.
 *
 * Requirements:
 * - 7.5: 일반 회원이 10권 이상의 책을 아카이브에 등록하면 칭호를 "컨트리뷰터"로 자동 변경
 * - 7.6: 회원의 등록 기여 횟수를 추적하고 저장
 */
@Service
@Transactional
public class ContributionServiceImpl implements ContributionService {

    private static final int CONTRIBUTOR_THRESHOLD = 10;

    private final UserRepository userRepository;
    private final OriginalBookRepository originalBookRepository;
    private final TranslatedBookRepository translatedBookRepository;
    private final RedesignRepository redesignRepository;

    public ContributionServiceImpl(
            UserRepository userRepository,
            OriginalBookRepository originalBookRepository,
            TranslatedBookRepository translatedBookRepository,
            RedesignRepository redesignRepository) {
        this.userRepository = userRepository;
        this.originalBookRepository = originalBookRepository;
        this.translatedBookRepository = translatedBookRepository;
        this.redesignRepository = redesignRepository;
    }

    @Override
    public User incrementContribution(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // 문제 4 해결: 원자적 UPDATE 쿼리로 동시성 문제 방지
        userRepository.incrementContributionCount(userId);

        // 칭호 업데이트 (CONTRIBUTOR 임계값 도달 시)
        userRepository.updateTitleIfEligible(userId, CONTRIBUTOR_THRESHOLD, UserTitle.CONTRIBUTOR);

        // 최신 상태 반환
        return userRepository.findById(userId).orElse(user);
    }

    @Override
    public User decrementContribution(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // 문제 4 해결: 원자적 UPDATE 쿼리로 동시성 문제 방지
        userRepository.decrementContributionCount(userId);

        // 최신 상태 반환
        return userRepository.findById(userId).orElse(user);
    }

    @Override
    public User recalculateContribution(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // 원서, 번역서, 리디자인 등록 수 합산
        long originalBookCount = originalBookRepository.countByCreatedById(userId);
        long translatedBookCount = translatedBookRepository.countByCreatedById(userId);
        long redesignCount = redesignRepository.countByCreatedById(userId);

        int totalContribution = (int) (originalBookCount + translatedBookCount + redesignCount);
        user.setContributionCount(totalContribution);
        checkAndUpdateTitle(user);

        return userRepository.save(user);
    }

    @Override
    public User updateTitle(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        checkAndUpdateTitle(user);
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public int getContributionCount(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return user.getContributionCount();
    }

    /**
     * 기여 횟수에 따라 칭호를 업데이트합니다.
     * 일반 회원(MEMBER 역할)이 10권 이상 등록하면 CONTRIBUTOR 칭호를 부여합니다.
     */
    private void checkAndUpdateTitle(User user) {
        // 일반 회원만 컨트리뷰터 칭호 부여 대상
        if (user.getRole() == UserRole.MEMBER) {
            if (user.getContributionCount() >= CONTRIBUTOR_THRESHOLD) {
                user.setTitle(UserTitle.CONTRIBUTOR);
            } else {
                user.setTitle(UserTitle.MEMBER);
            }
        }
    }
}
