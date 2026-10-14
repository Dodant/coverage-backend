package com.bookcover.archive.repository;

import com.bookcover.archive.domain.user.User;
import com.bookcover.archive.domain.user.UserRole;
import com.bookcover.archive.domain.user.UserStatus;
import com.bookcover.archive.domain.user.UserTitle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * 회원 Repository.
 * 회원 엔티티에 대한 데이터 접근을 제공합니다.
 *
 * Requirements:
 * - 4.1: 회원이 북커버에 좋아요를 누르면 해당 회원의 좋아요를 기록
 * - 5.1: 회원이 댓글을 작성하면 댓글 내용, 작성자, 작성 시간을 저장
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * 이메일로 회원을 조회합니다.
     *
     * @param email 이메일
     * @return 회원 Optional
     */
    Optional<User> findByEmail(String email);

    /**
     * 이메일이 존재하는지 확인합니다.
     *
     * @param email 이메일
     * @return 존재 여부
     */
    boolean existsByEmail(String email);

    /**
     * 역할로 회원 목록을 조회합니다.
     *
     * @param role 회원 역할
     * @param pageable 페이징 정보
     * @return 회원 페이지
     */
    Page<User> findByRole(UserRole role, Pageable pageable);

    /**
     * 칭호로 회원 목록을 조회합니다.
     *
     * @param title 회원 칭호
     * @param pageable 페이징 정보
     * @return 회원 페이지
     */
    Page<User> findByTitle(UserTitle title, Pageable pageable);

    /**
     * 상태로 회원 목록을 조회합니다.
     *
     * @param status 회원 상태
     * @param pageable 페이징 정보
     * @return 회원 페이지
     */
    Page<User> findByStatus(UserStatus status, Pageable pageable);

    /**
     * 표시 이름으로 회원을 검색합니다 (대소문자 무시, 지정 상태 제외).
     *
     * @param displayName 표시 이름 검색어
     * @param status 제외할 상태 (탈퇴 회원 제외에 사용)
     * @param pageable 페이징 정보
     * @return 회원 페이지
     */
    Page<User> findByDisplayNameContainingIgnoreCaseAndStatusNot(String displayName, UserStatus status, Pageable pageable);

    /**
     * 신고 제한된 회원 목록을 조회합니다.
     *
     * @param reportRestricted 신고 제한 여부
     * @param pageable 페이징 정보
     * @return 회원 페이지
     */
    Page<User> findByReportRestricted(Boolean reportRestricted, Pageable pageable);

    /**
     * 정지 기간이 지난 회원 목록을 조회합니다.
     *
     * @param now 현재 시간
     * @param pageable 페이징 정보
     * @return 회원 페이지
     */
    Page<User> findBySuspendedUntilBefore(Instant now, Pageable pageable);

    /**
     * 현재 정지 중인 회원 목록을 조회합니다.
     *
     * @param now 현재 시간
     * @param pageable 페이징 정보
     * @return 회원 페이지
     */
    Page<User> findBySuspendedUntilAfter(Instant now, Pageable pageable);

    /**
     * 기여 횟수가 특정 값 이상인 회원 목록을 조회합니다.
     *
     * @param count 최소 기여 횟수
     * @param pageable 페이징 정보
     * @return 회원 페이지
     */
    Page<User> findByContributionCountGreaterThanEqual(Integer count, Pageable pageable);

    /**
     * 특정 기간 이후에 가입한 회원 목록을 조회합니다.
     *
     * @param createdAt 기준 시간
     * @param pageable 페이징 정보
     * @return 회원 페이지
     */
    Page<User> findByCreatedAtAfter(Instant createdAt, Pageable pageable);

    /**
     * 역할별 회원 수를 조회합니다.
     *
     * @param role 회원 역할
     * @return 회원 수
     */
    long countByRole(UserRole role);

    /**
     * 상태별 회원 수를 조회합니다.
     *
     * @param status 회원 상태
     * @return 회원 수
     */
    long countByStatus(UserStatus status);

    /**
     * 문제 4 해결: 기여 횟수를 원자적으로 1 증가시킵니다 (동시성 문제 방지).
     *
     * @param userId 회원 ID
     * @return 업데이트된 행 수
     */
    @Modifying
    @Query("UPDATE User u SET u.contributionCount = u.contributionCount + 1 WHERE u.id = :userId")
    int incrementContributionCount(@Param("userId") UUID userId);

    /**
     * 문제 4 해결: 기여 횟수를 원자적으로 1 감소시킵니다 (동시성 문제 방지, 0 미만 방지).
     *
     * @param userId 회원 ID
     * @return 업데이트된 행 수
     */
    @Modifying
    @Query("UPDATE User u SET u.contributionCount = u.contributionCount - 1 WHERE u.id = :userId AND u.contributionCount > 0")
    int decrementContributionCount(@Param("userId") UUID userId);

    /**
     * 기여 횟수가 특정 값 이상인 일반 회원의 칭호를 CONTRIBUTOR로 업데이트합니다.
     *
     * @param userId 회원 ID
     * @param threshold 기여 횟수 임계값
     * @param newTitle 새 칭호
     * @return 업데이트된 행 수
     */
    @Modifying
    @Query("UPDATE User u SET u.title = :newTitle WHERE u.id = :userId AND u.role = 'MEMBER' AND u.contributionCount >= :threshold")
    int updateTitleIfEligible(@Param("userId") UUID userId, @Param("threshold") int threshold, @Param("newTitle") UserTitle newTitle);
}
