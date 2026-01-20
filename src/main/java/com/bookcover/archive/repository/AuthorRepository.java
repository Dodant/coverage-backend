package com.bookcover.archive.repository;

import com.bookcover.archive.domain.contributor.Author;
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
 * 작가 Repository.
 * 작가 엔티티에 대한 데이터 접근을 제공합니다.
 */
@Repository
public interface AuthorRepository extends JpaRepository<Author, UUID> {

    /**
     * 이름으로 작가를 조회합니다.
     *
     * @param name 작가 이름
     * @return 작가 Optional
     */
    Optional<Author> findByName(String name);

    /**
     * 이름이 존재하는지 확인합니다.
     *
     * @param name 작가 이름
     * @return 존재 여부
     */
    boolean existsByName(String name);

    /**
     * 이름으로 작가를 검색합니다 (대소문자 무시).
     *
     * @param name 이름 검색어
     * @param pageable 페이징 정보
     * @return 작가 페이지
     */
    Page<Author> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * 국적으로 작가 목록을 조회합니다.
     *
     * @param nationality 국적 코드
     * @param pageable 페이징 정보
     * @return 작가 페이지
     */
    Page<Author> findByNationality(String nationality, Pageable pageable);

    /**
     * 이름 또는 별칭으로 작가를 검색합니다.
     *
     * @param name 이름 검색어
     * @return 작가 목록
     */
    @Query("SELECT a FROM Author a WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "OR EXISTS (SELECT 1 FROM a.aliases alias WHERE LOWER(alias) LIKE LOWER(CONCAT('%', :name, '%')))")
    List<Author> findByNameOrAlias(@Param("name") String name);
}
