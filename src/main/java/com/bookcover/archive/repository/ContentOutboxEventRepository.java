package com.bookcover.archive.repository;

import com.bookcover.archive.event.ContentOutboxEvent;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;

/** 미발행 콘텐츠 이벤트를 생성 순서로 가져온다. */
public interface ContentOutboxEventRepository extends JpaRepository<ContentOutboxEvent, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM ContentOutboxEvent e WHERE e.publishedAt IS NULL ORDER BY e.createdAt, e.id")
    List<ContentOutboxEvent> findNextForUpdate(Pageable pageable);
}
