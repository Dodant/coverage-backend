package com.bookcover.archive.repository;

import com.bookcover.archive.domain.book.CoverCredit;
import com.bookcover.archive.domain.contributor.Designer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * 표지 크레딧 리포지토리.
 */
@Repository
public interface CoverCreditRepository extends JpaRepository<CoverCredit, UUID> {

    /** 디자이너가 앞면 변형·뒷면 표지 크레딧에 오른 원서 (병합 후 검색 재색인 대상) */
    @Query("SELECT ob.id FROM OriginalBook ob WHERE EXISTS (SELECT 1 FROM CoverCredit cr " +
        "WHERE cr.designer.id = :designerId AND (cr.cover.book = ob OR cr.cover = ob.backCover))")
    List<UUID> findOriginalBookIds(@Param("designerId") UUID designerId);

    /** 디자이너가 앞면 변형·뒷면 표지 크레딧에 오른 번역서 (병합 후 검색 재색인 대상) */
    @Query("SELECT tb.id FROM TranslatedBook tb WHERE EXISTS (SELECT 1 FROM CoverCredit cr " +
        "WHERE cr.designer.id = :designerId AND (cr.cover.book = tb OR cr.cover = tb.backCover))")
    List<UUID> findTranslatedBookIds(@Param("designerId") UUID designerId);

    /** 병합 전, target이 이미 같은 표지·역할로 올라 있는 source 크레딧을 지운다 (유니크 제약 충돌 방지) */
    @Modifying
    @Query("DELETE FROM CoverCredit cr WHERE cr.designer.id = :sourceId AND EXISTS (" +
        "SELECT 1 FROM CoverCredit t WHERE t.designer.id = :targetId AND t.cover = cr.cover AND t.role = cr.role)")
    int deleteOverlapping(@Param("sourceId") UUID sourceId, @Param("targetId") UUID targetId);

    @Modifying
    @Query("UPDATE CoverCredit cr SET cr.designer = :target WHERE cr.designer.id = :sourceId")
    int reassign(@Param("sourceId") UUID sourceId, @Param("target") Designer target);
}
