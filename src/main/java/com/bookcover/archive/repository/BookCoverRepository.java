package com.bookcover.archive.repository;

import com.bookcover.archive.domain.book.BookCover;
import com.bookcover.archive.domain.book.CoverLayout;
import com.bookcover.archive.domain.book.CoverType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * 북커버 Repository.
 * 북커버 엔티티에 대한 데이터 접근을 제공합니다.
 */
@Repository
public interface BookCoverRepository extends JpaRepository<BookCover, UUID> {

    /**
     * 커버 유형으로 북커버 목록을 조회합니다.
     *
     * @param coverType 커버 유형
     * @param pageable 페이징 정보
     * @return 북커버 페이지
     */
    Page<BookCover> findByCoverType(CoverType coverType, Pageable pageable);

    /**
     * 레이아웃으로 북커버 목록을 조회합니다.
     *
     * @param layout 레이아웃
     * @param pageable 페이징 정보
     * @return 북커버 페이지
     */
    Page<BookCover> findByLayout(CoverLayout layout, Pageable pageable);

    /**
     * 대표 색상으로 북커버 목록을 조회합니다.
     *
     * @param dominantColor 대표 색상 (HEX)
     * @param pageable 페이징 정보
     * @return 북커버 페이지
     */
    Page<BookCover> findByDominantColor(String dominantColor, Pageable pageable);

    /**
     * 좋아요 수 기준으로 정렬된 북커버 목록을 조회합니다.
     *
     * @param pageable 페이징 정보
     * @return 북커버 페이지
     */
    Page<BookCover> findAllByOrderByLikeCountDesc(Pageable pageable);

    /**
     * 이미지 URL로 북커버를 조회합니다.
     *
     * @param imageUrl 이미지 URL
     * @return 북커버 목록
     */
    List<BookCover> findByImageUrl(String imageUrl);
}
