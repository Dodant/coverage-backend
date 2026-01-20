package com.bookcover.archive.repository;

import com.bookcover.archive.domain.tag.DesignTag;
import com.bookcover.archive.domain.tag.TagCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 디자인 태그 Repository.
 * 디자인 태그 엔티티에 대한 데이터 접근을 제공합니다.
 *
 * Requirements:
 * - 20.1: 커버에 대해 디자인 속성 태그를 부여할 수 있게 한다
 */
@Repository
public interface DesignTagRepository extends JpaRepository<DesignTag, UUID> {

    /**
     * 이름으로 태그를 조회합니다.
     *
     * @param name 태그 이름
     * @return 태그 Optional
     */
    Optional<DesignTag> findByName(String name);

    /**
     * 이름이 존재하는지 확인합니다.
     *
     * @param name 태그 이름
     * @return 존재 여부
     */
    boolean existsByName(String name);

    /**
     * 카테고리로 태그 목록을 조회합니다.
     *
     * @param category 태그 카테고리
     * @return 태그 목록
     */
    List<DesignTag> findByCategory(TagCategory category);

    /**
     * 카테고리로 태그 목록을 페이징하여 조회합니다.
     *
     * @param category 태그 카테고리
     * @param pageable 페이징 정보
     * @return 태그 페이지
     */
    Page<DesignTag> findByCategory(TagCategory category, Pageable pageable);

    /**
     * 승인 상태로 태그 목록을 조회합니다.
     *
     * @param approved 승인 여부
     * @return 태그 목록
     */
    List<DesignTag> findByApproved(Boolean approved);

    /**
     * 승인 상태로 태그 목록을 페이징하여 조회합니다.
     *
     * @param approved 승인 여부
     * @param pageable 페이징 정보
     * @return 태그 페이지
     */
    Page<DesignTag> findByApproved(Boolean approved, Pageable pageable);

    /**
     * 카테고리와 승인 상태로 태그 목록을 조회합니다.
     *
     * @param category 태그 카테고리
     * @param approved 승인 여부
     * @return 태그 목록
     */
    List<DesignTag> findByCategoryAndApproved(TagCategory category, Boolean approved);

    /**
     * 이름으로 태그를 검색합니다 (대소문자 무시).
     *
     * @param name 이름 검색어
     * @param pageable 페이징 정보
     * @return 태그 페이지
     */
    Page<DesignTag> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * 승인된 태그 중 이름으로 검색합니다.
     *
     * @param name 이름 검색어
     * @param approved 승인 여부
     * @param pageable 페이징 정보
     * @return 태그 페이지
     */
    Page<DesignTag> findByNameContainingIgnoreCaseAndApproved(String name, Boolean approved, Pageable pageable);

    /**
     * 사용 횟수 기준으로 정렬된 태그 목록을 조회합니다.
     *
     * @param approved 승인 여부
     * @param pageable 페이징 정보
     * @return 태그 페이지
     */
    Page<DesignTag> findByApprovedOrderByUsageCountDesc(Boolean approved, Pageable pageable);

    /**
     * 카테고리별 태그 수를 조회합니다.
     *
     * @param category 태그 카테고리
     * @return 태그 수
     */
    long countByCategory(TagCategory category);

    /**
     * 승인 상태별 태그 수를 조회합니다.
     *
     * @param approved 승인 여부
     * @return 태그 수
     */
    long countByApproved(Boolean approved);

    /**
     * 인기 태그 목록을 조회합니다 (사용 횟수 기준 내림차순).
     *
     * @param pageable 페이징 정보
     * @return 태그 페이지
     */
    Page<DesignTag> findAllByOrderByUsageCountDesc(Pageable pageable);

    /**
     * 승인된 인기 태그 목록을 조회합니다.
     *
     * @param approved 승인 여부
     * @param pageable 페이징 정보
     * @return 태그 페이지
     */
    Page<DesignTag> findByApprovedOrderByUsageCountDesc(boolean approved, Pageable pageable);

    /**
     * 승인된 태그 목록을 사용 횟수 기준으로 정렬하여 조회합니다.
     *
     * @return 태그 목록
     */
    List<DesignTag> findByApprovedTrueOrderByUsageCountDesc();
}
