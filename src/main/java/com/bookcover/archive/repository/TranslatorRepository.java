package com.bookcover.archive.repository;

import com.bookcover.archive.domain.contributor.Translator;
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
 * 옮긴이(번역가) Repository.
 * 옮긴이 엔티티에 대한 데이터 접근을 제공합니다.
 */
@Repository
public interface TranslatorRepository extends JpaRepository<Translator, UUID> {

    /**
     * 이름으로 옮긴이를 조회합니다.
     *
     * @param name 옮긴이 이름
     * @return 옮긴이 Optional
     */
    Optional<Translator> findByName(String name);

    /**
     * 이름이 존재하는지 확인합니다.
     *
     * @param name 옮긴이 이름
     * @return 존재 여부
     */
    boolean existsByName(String name);

    /**
     * 이름으로 옮긴이를 검색합니다 (대소문자 무시).
     *
     * @param name 이름 검색어
     * @param pageable 페이징 정보
     * @return 옮긴이 페이지
     */
    Page<Translator> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * 국적으로 옮긴이 목록을 조회합니다.
     *
     * @param nationality 국적 코드
     * @param pageable 페이징 정보
     * @return 옮긴이 페이지
     */
    Page<Translator> findByNationality(String nationality, Pageable pageable);

    /**
     * 이름 또는 별칭으로 옮긴이를 검색합니다.
     *
     * @param name 이름 검색어
     * @return 옮긴이 목록
     */
    @Query("SELECT t FROM Translator t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "OR EXISTS (SELECT 1 FROM t.aliases alias WHERE LOWER(alias) LIKE LOWER(CONCAT('%', :name, '%')))")
    List<Translator> findByNameOrAlias(@Param("name") String name);
}
