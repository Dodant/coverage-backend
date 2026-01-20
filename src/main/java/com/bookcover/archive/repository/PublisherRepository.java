package com.bookcover.archive.repository;

import com.bookcover.archive.domain.contributor.Publisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 출판사 Repository.
 * 출판사 엔티티에 대한 데이터 접근을 제공합니다.
 */
@Repository
public interface PublisherRepository extends JpaRepository<Publisher, UUID> {

    /**
     * 이름으로 출판사를 조회합니다.
     *
     * @param name 출판사 이름
     * @return 출판사 Optional
     */
    Optional<Publisher> findByName(String name);

    /**
     * 이름이 존재하는지 확인합니다.
     *
     * @param name 출판사 이름
     * @return 존재 여부
     */
    boolean existsByName(String name);

    /**
     * 이름으로 출판사를 검색합니다 (대소문자 무시).
     *
     * @param name 이름 검색어
     * @param pageable 페이징 정보
     * @return 출판사 페이지
     */
    Page<Publisher> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * 국가 코드로 출판사 목록을 조회합니다.
     *
     * @param countryCode 국가 코드
     * @param pageable 페이징 정보
     * @return 출판사 페이지
     */
    Page<Publisher> findByCountryCode(String countryCode, Pageable pageable);

    /**
     * 이름 또는 별칭으로 출판사를 검색합니다.
     *
     * @param name 이름 검색어
     * @return 출판사 목록
     */
    @Query("SELECT p FROM Publisher p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "OR EXISTS (SELECT 1 FROM p.aliases alias WHERE LOWER(alias) LIKE LOWER(CONCAT('%', :name, '%')))")
    List<Publisher> findByNameOrAlias(@Param("name") String name);
}
