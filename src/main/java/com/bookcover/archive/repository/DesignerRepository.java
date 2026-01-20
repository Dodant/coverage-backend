package com.bookcover.archive.repository;

import com.bookcover.archive.domain.contributor.Designer;
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
 * 디자이너 Repository.
 * 디자이너 엔티티에 대한 데이터 접근을 제공합니다.
 */
@Repository
public interface DesignerRepository extends JpaRepository<Designer, UUID> {

    /**
     * 이름으로 디자이너를 조회합니다.
     *
     * @param name 디자이너 이름
     * @return 디자이너 Optional
     */
    Optional<Designer> findByName(String name);

    /**
     * 이름이 존재하는지 확인합니다.
     *
     * @param name 디자이너 이름
     * @return 존재 여부
     */
    boolean existsByName(String name);

    /**
     * 이름으로 디자이너를 검색합니다 (대소문자 무시).
     *
     * @param name 이름 검색어
     * @param pageable 페이징 정보
     * @return 디자이너 페이지
     */
    Page<Designer> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * 국적으로 디자이너 목록을 조회합니다.
     *
     * @param nationality 국적 코드
     * @param pageable 페이징 정보
     * @return 디자이너 페이지
     */
    Page<Designer> findByNationality(String nationality, Pageable pageable);

    /**
     * 이름 또는 별칭으로 디자이너를 검색합니다.
     *
     * @param name 이름 검색어
     * @return 디자이너 목록
     */
    @Query("SELECT d FROM Designer d WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "OR EXISTS (SELECT 1 FROM d.aliases alias WHERE LOWER(alias) LIKE LOWER(CONCAT('%', :name, '%')))")
    List<Designer> findByNameOrAlias(@Param("name") String name);
}
