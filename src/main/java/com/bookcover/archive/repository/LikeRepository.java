package com.bookcover.archive.repository;

import com.bookcover.archive.domain.social.Like;
import com.bookcover.archive.domain.social.LikeTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 좋아요 Repository.
 * 좋아요 엔티티에 대한 데이터 접근을 제공합니다.
 *
 * Requirements:
 * - 4.1: 회원이 북커버에 좋아요를 누르면 해당 회원의 좋아요를 기록하고 좋아요 수를 증가시킨다
 */
@Repository
public interface LikeRepository extends JpaRepository<Like, UUID> {

    /**
     * 회원 ID, 대상 유형, 대상 ID로 좋아요를 조회합니다.
     *
     * @param userId 회원 ID
     * @param targetType 대상 유형
     * @param targetId 대상 ID
     * @return 좋아요 Optional
     */
    Optional<Like> findByUserIdAndTargetTypeAndTargetId(UUID userId, LikeTargetType targetType, UUID targetId);

    /**
     * 회원이 특정 대상에 좋아요를 눌렀는지 확인합니다.
     *
     * @param userId 회원 ID
     * @param targetType 대상 유형
     * @param targetId 대상 ID
     * @return 좋아요 여부
     */
    boolean existsByUserIdAndTargetTypeAndTargetId(UUID userId, LikeTargetType targetType, UUID targetId);

    /**
     * 대상 유형과 대상 ID로 좋아요 수를 조회합니다.
     *
     * @param targetType 대상 유형
     * @param targetId 대상 ID
     * @return 좋아요 수
     */
    long countByTargetTypeAndTargetId(LikeTargetType targetType, UUID targetId);

    /**
     * 회원 ID로 좋아요 목록을 조회합니다.
     *
     * @param userId 회원 ID
     * @param pageable 페이징 정보
     * @return 좋아요 페이지
     */
    Page<Like> findByUserId(UUID userId, Pageable pageable);

    /**
     * 회원 ID와 대상 유형으로 좋아요 목록을 조회합니다.
     *
     * @param userId 회원 ID
     * @param targetType 대상 유형
     * @param pageable 페이징 정보
     * @return 좋아요 페이지
     */
    Page<Like> findByUserIdAndTargetType(UUID userId, LikeTargetType targetType, Pageable pageable);

    /**
     * 대상 유형과 대상 ID로 좋아요 목록을 조회합니다.
     *
     * @param targetType 대상 유형
     * @param targetId 대상 ID
     * @param pageable 페이징 정보
     * @return 좋아요 페이지
     */
    Page<Like> findByTargetTypeAndTargetId(LikeTargetType targetType, UUID targetId, Pageable pageable);

    /**
     * 회원 ID, 대상 유형, 대상 ID로 좋아요를 삭제합니다.
     *
     * @param userId 회원 ID
     * @param targetType 대상 유형
     * @param targetId 대상 ID
     */
    void deleteByUserIdAndTargetTypeAndTargetId(UUID userId, LikeTargetType targetType, UUID targetId);

    /**
     * 대상 유형과 대상 ID로 모든 좋아요를 삭제합니다.
     *
     * @param targetType 대상 유형
     * @param targetId 대상 ID
     */
    void deleteByTargetTypeAndTargetId(LikeTargetType targetType, UUID targetId);

    /**
     * 회원 ID로 좋아요 수를 조회합니다.
     *
     * @param userId 회원 ID
     * @return 좋아요 수
     */
    long countByUserId(UUID userId);

    /**
     * 특정 기간 내에 회원이 누른 좋아요 수를 조회합니다 (레이트 리밋용).
     *
     * @param userId 회원 ID
     * @param after 시작 시간
     * @return 좋아요 수
     */
    long countByUserIdAndCreatedAtAfter(UUID userId, Instant after);

    /**
     * 회원이 좋아요한 대상 ID 목록을 조회합니다.
     *
     * @param userId 회원 ID
     * @param targetType 대상 유형
     * @return 대상 ID 목록
     */
    @Query("SELECT l.targetId FROM Like l WHERE l.user.id = :userId AND l.targetType = :targetType")
    List<UUID> findTargetIdsByUserIdAndTargetType(@Param("userId") UUID userId, @Param("targetType") LikeTargetType targetType);
}
