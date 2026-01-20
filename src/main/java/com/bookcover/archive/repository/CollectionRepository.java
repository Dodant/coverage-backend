package com.bookcover.archive.repository;

import com.bookcover.archive.domain.collection.Collection;
import com.bookcover.archive.domain.collection.CollectionVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * 컬렉션 Repository.
 * 컬렉션 엔티티에 대한 데이터 접근을 제공합니다.
 *
 * Requirements:
 * - 11.2: 회원이 북커버를 컬렉션에 추가하면 해당 북커버를 컬렉션과 연결한다
 */
@Repository
public interface CollectionRepository extends JpaRepository<Collection, UUID> {

    /**
     * 공유 토큰으로 컬렉션을 조회합니다.
     *
     * @param shareToken 공유 토큰
     * @return 컬렉션 Optional
     */
    Optional<Collection> findByShareToken(String shareToken);

    /**
     * 소유자 ID로 컬렉션 목록을 조회합니다.
     *
     * @param ownerId 소유자 ID
     * @param pageable 페이징 정보
     * @return 컬렉션 페이지
     */
    Page<Collection> findByOwnerId(UUID ownerId, Pageable pageable);

    /**
     * 소유자 ID와 가시성으로 컬렉션 목록을 조회합니다.
     *
     * @param ownerId 소유자 ID
     * @param visibility 가시성
     * @param pageable 페이징 정보
     * @return 컬렉션 페이지
     */
    Page<Collection> findByOwnerIdAndVisibility(UUID ownerId, CollectionVisibility visibility, Pageable pageable);

    /**
     * 가시성으로 컬렉션 목록을 조회합니다.
     *
     * @param visibility 가시성
     * @param pageable 페이징 정보
     * @return 컬렉션 페이지
     */
    Page<Collection> findByVisibility(CollectionVisibility visibility, Pageable pageable);

    /**
     * 이름으로 컬렉션을 검색합니다 (대소문자 무시).
     *
     * @param name 이름 검색어
     * @param pageable 페이징 정보
     * @return 컬렉션 페이지
     */
    Page<Collection> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * 공개 컬렉션 중 이름으로 검색합니다.
     *
     * @param name 이름 검색어
     * @param visibility 가시성
     * @param pageable 페이징 정보
     * @return 컬렉션 페이지
     */
    Page<Collection> findByNameContainingIgnoreCaseAndVisibility(String name, CollectionVisibility visibility, Pageable pageable);

    /**
     * 소유자 ID로 컬렉션 수를 조회합니다.
     *
     * @param ownerId 소유자 ID
     * @return 컬렉션 수
     */
    long countByOwnerId(UUID ownerId);

    /**
     * 가시성별 컬렉션 수를 조회합니다.
     *
     * @param visibility 가시성
     * @return 컬렉션 수
     */
    long countByVisibility(CollectionVisibility visibility);

    /**
     * 소유자 ID와 이름으로 컬렉션이 존재하는지 확인합니다.
     *
     * @param ownerId 소유자 ID
     * @param name 컬렉션 이름
     * @return 존재 여부
     */
    boolean existsByOwnerIdAndName(UUID ownerId, String name);
}
