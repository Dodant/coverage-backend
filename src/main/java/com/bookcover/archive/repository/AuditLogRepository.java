package com.bookcover.archive.repository;

import com.bookcover.archive.domain.audit.AuditAction;
import com.bookcover.archive.domain.audit.AuditEntityType;
import com.bookcover.archive.domain.audit.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 감사 로그 Repository.
 * 감사 로그 엔티티에 대한 데이터 접근을 제공합니다.
 *
 * Requirements:
 * - 16.2: 엔티티 상세 페이지에서 "수정 이력"을 조회할 수 있게 한다
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * 엔티티 유형과 엔티티 ID로 감사 로그 목록을 조회합니다.
     *
     * @param entityType 엔티티 유형
     * @param entityId 엔티티 ID
     * @return 감사 로그 목록
     */
    List<AuditLog> findByEntityTypeAndEntityId(AuditEntityType entityType, UUID entityId);

    /**
     * 엔티티 유형과 엔티티 ID로 감사 로그 목록을 페이징하여 조회합니다.
     *
     * @param entityType 엔티티 유형
     * @param entityId 엔티티 ID
     * @param pageable 페이징 정보
     * @return 감사 로그 페이지
     */
    Page<AuditLog> findByEntityTypeAndEntityId(AuditEntityType entityType, UUID entityId, Pageable pageable);

    /**
     * 엔티티 유형과 엔티티 ID로 감사 로그 목록을 시간순으로 조회합니다.
     *
     * @param entityType 엔티티 유형
     * @param entityId 엔티티 ID
     * @return 감사 로그 목록 (시간순)
     */
    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(AuditEntityType entityType, UUID entityId);

    /**
     * 수행자 ID로 감사 로그 목록을 조회합니다.
     *
     * @param performedById 수행자 ID
     * @param pageable 페이징 정보
     * @return 감사 로그 페이지
     */
    Page<AuditLog> findByPerformedById(UUID performedById, Pageable pageable);

    /**
     * 액션 유형으로 감사 로그 목록을 조회합니다.
     *
     * @param action 액션 유형
     * @param pageable 페이징 정보
     * @return 감사 로그 페이지
     */
    Page<AuditLog> findByAction(AuditAction action, Pageable pageable);

    /**
     * 엔티티 유형으로 감사 로그 목록을 조회합니다.
     *
     * @param entityType 엔티티 유형
     * @param pageable 페이징 정보
     * @return 감사 로그 페이지
     */
    Page<AuditLog> findByEntityType(AuditEntityType entityType, Pageable pageable);

    /**
     * 특정 기간 내의 감사 로그 목록을 조회합니다.
     *
     * @param start 시작 시간
     * @param end 종료 시간
     * @param pageable 페이징 정보
     * @return 감사 로그 페이지
     */
    Page<AuditLog> findByCreatedAtBetween(Instant start, Instant end, Pageable pageable);

    /**
     * 특정 시간 이후의 감사 로그 목록을 조회합니다.
     *
     * @param after 시작 시간
     * @param pageable 페이징 정보
     * @return 감사 로그 페이지
     */
    Page<AuditLog> findByCreatedAtAfter(Instant after, Pageable pageable);

    /**
     * 특정 시간 이전의 감사 로그를 삭제합니다 (보관 기간 만료).
     *
     * @param before 기준 시간
     */
    void deleteByCreatedAtBefore(Instant before);

    /**
     * 엔티티 유형과 액션으로 감사 로그 목록을 조회합니다.
     *
     * @param entityType 엔티티 유형
     * @param action 액션 유형
     * @param pageable 페이징 정보
     * @return 감사 로그 페이지
     */
    Page<AuditLog> findByEntityTypeAndAction(AuditEntityType entityType, AuditAction action, Pageable pageable);

    /**
     * 액션별 감사 로그 수를 조회합니다.
     *
     * @param action 액션 유형
     * @return 감사 로그 수
     */
    long countByAction(AuditAction action);

    /**
     * 엔티티 유형별 감사 로그 수를 조회합니다.
     *
     * @param entityType 엔티티 유형
     * @return 감사 로그 수
     */
    long countByEntityType(AuditEntityType entityType);

    /**
     * 최근 감사 로그 목록을 조회합니다.
     *
     * @param pageable 페이징 정보
     * @return 감사 로그 페이지
     */
    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
