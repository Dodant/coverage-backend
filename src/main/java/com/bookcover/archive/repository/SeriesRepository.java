package com.bookcover.archive.repository;

import com.bookcover.archive.domain.book.Series;
import com.bookcover.archive.domain.media.Media;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 시리즈 리포지토리.
 */
@Repository
public interface SeriesRepository extends JpaRepository<Series, UUID> {

    /** 중복 행이 있어도 예외 없이 먼저 만든 시리즈를 고른다 */
    Optional<Series> findFirstByNameAndPublisherIdOrderByCreatedAtAsc(String name, UUID publisherId);

    Optional<Series> findFirstByNameAndPublisherIsNullOrderByCreatedAtAsc(String name);

    List<Series> findByPublisherId(UUID publisherId);

    Page<Series> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /** 시리즈에 속한 공개(PUBLISHED) 원서·번역서. 권 번호는 문자열이라 정렬은 호출 쪽에서 한다 */
    @Query("SELECT m FROM Media m WHERE m.series.id = :seriesId AND m.status = com.bookcover.archive.domain.common.ContentStatus.PUBLISHED")
    List<Media> findBooks(@Param("seriesId") UUID seriesId);

    /** 시리즈 병합: source 시리즈의 책을 target 시리즈로 옮긴다 (앞서 바꾼 책 엔티티를 먼저 flush) */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE Media m SET m.series = :target WHERE m.series = :source")
    int moveBooks(@Param("source") Series source, @Param("target") Series target);
}
