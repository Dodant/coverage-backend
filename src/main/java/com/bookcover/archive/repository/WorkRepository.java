package com.bookcover.archive.repository;

import com.bookcover.archive.domain.book.Work;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * 작품 Repository.
 * 작품 엔티티에 대한 데이터 접근을 제공합니다.
 */
@Repository
public interface WorkRepository extends JpaRepository<Work, UUID> {

    /**
     * 제목으로 작품을 검색합니다 (대소문자 무시).
     *
     * @param title 제목 검색어
     * @param pageable 페이징 정보
     * @return 작품 페이지
     */
    Page<Work> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    /**
     * 원어 제목으로 작품을 검색합니다 (대소문자 무시).
     *
     * @param originalTitle 원어 제목 검색어
     * @param pageable 페이징 정보
     * @return 작품 페이지
     */
    Page<Work> findByOriginalTitleContainingIgnoreCase(String originalTitle, Pageable pageable);

    /**
     * 제목으로 작품을 조회합니다.
     *
     * @param title 제목
     * @return 작품 Optional
     */
    Optional<Work> findByTitle(String title);

    /**
     * 제목이 존재하는지 확인합니다.
     *
     * @param title 제목
     * @return 존재 여부
     */
    boolean existsByTitle(String title);
}
