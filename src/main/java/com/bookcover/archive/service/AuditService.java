package com.bookcover.archive.service;

import com.bookcover.archive.domain.audit.AuditAction;
import com.bookcover.archive.domain.audit.AuditEntityType;
import com.bookcover.archive.domain.audit.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 감사 로그 서비스 인터페이스.
 * 모든 데이터 변경 이력을 추적하고 조회하는 기능을 제공합니다.
 *
 * Requirements: 16.1, 16.2, 16.5
 */
public interface AuditService {

    /**
     * 감사 로그를 기록합니다.
     *
     * @param action 액션 유형
     * @param entityType 엔티티 유형
     * @param entityId 엔티티 ID
     * @param performedById 수행자 ID
     * @param previousValue 변경 전 값 (JSON)
     * @param newValue 변경 후 값 (JSON)
     * @return 생성된 감사 로그
     */
    AuditLog log(AuditAction action, AuditEntityType entityType, UUID entityId,
                 UUID performedById, String previousValue, String newValue);

    /**
     * 엔티티 생성 로그를 기록합니다.
     *
     * @param entityType 엔티티 유형
     * @param entityId 엔티티 ID
     * @param performedById 수행자 ID
     * @param newValue 생성된 값 (JSON)
     * @return 생성된 감사 로그
     */
    AuditLog logCreate(AuditEntityType entityType, UUID entityId, UUID performedById, String newValue);

    /**
     * 엔티티 수정 로그를 기록합니다.
     *
     * @param entityType 엔티티 유형
     * @param entityId 엔티티 ID
     * @param performedById 수행자 ID
     * @param previousValue 변경 전 값 (JSON)
     * @param newValue 변경 후 값 (JSON)
     * @return 생성된 감사 로그
     */
    AuditLog logUpdate(AuditEntityType entityType, UUID entityId, UUID performedById,
                       String previousValue, String newValue);

    /**
     * 엔티티 삭제 로그를 기록합니다.
     *
     * @param entityType 엔티티 유형
     * @param entityId 엔티티 ID
     * @param performedById 수행자 ID
     * @param previousValue 삭제 전 값 (JSON)
     * @return 생성된 감사 로그
     */
    AuditLog logDelete(AuditEntityType entityType, UUID entityId, UUID performedById, String previousValue);

    /**
     * 상태 변경 로그를 기록합니다.
     *
     * @param entityType 엔티티 유형
     * @param entityId 엔티티 ID
     * @param performedById 수행자 ID
     * @param previousStatus 변경 전 상태
     * @param newStatus 변경 후 상태
     * @return 생성된 감사 로그
     */
    AuditLog logStatusChange(AuditEntityType entityType, UUID entityId, UUID performedById,
                             String previousStatus, String newStatus);

    /**
     * 특정 엔티티의 수정 이력을 조회합니다.
     *
     * @param entityType 엔티티 유형
     * @param entityId 엔티티 ID
     * @return 감사 로그 목록 (시간순)
     */
    List<AuditLog> getEntityHistory(AuditEntityType entityType, UUID entityId);

    /**
     * 특정 엔티티의 수정 이력을 페이징하여 조회합니다.
     *
     * @param entityType 엔티티 유형
     * @param entityId 엔티티 ID
     * @param pageable 페이징 정보
     * @return 감사 로그 페이지
     */
    Page<AuditLog> getEntityHistory(AuditEntityType entityType, UUID entityId, Pageable pageable);

    /**
     * 특정 사용자가 수행한 작업 이력을 조회합니다.
     *
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 감사 로그 페이지
     */
    Page<AuditLog> getUserActions(UUID userId, Pageable pageable);

    /**
     * 특정 액션 유형의 로그를 조회합니다.
     *
     * @param action 액션 유형
     * @param pageable 페이징 정보
     * @return 감사 로그 페이지
     */
    Page<AuditLog> getLogsByAction(AuditAction action, Pageable pageable);

    /**
     * 특정 기간 내의 로그를 조회합니다.
     *
     * @param start 시작 시간
     * @param end 종료 시간
     * @param pageable 페이징 정보
     * @return 감사 로그 페이지
     */
    Page<AuditLog> getLogsByDateRange(Instant start, Instant end, Pageable pageable);

    /**
     * 최근 감사 로그를 조회합니다.
     *
     * @param pageable 페이징 정보
     * @return 감사 로그 페이지
     */
    Page<AuditLog> getRecentLogs(Pageable pageable);

    /**
     * 보관 기간이 만료된 로그를 삭제합니다.
     * 감사 로그는 최소 1년간 보관됩니다.
     *
     * @param retentionDays 보관 기간 (일)
     * @return 삭제된 로그 수
     */
    long cleanupExpiredLogs(int retentionDays);
}
