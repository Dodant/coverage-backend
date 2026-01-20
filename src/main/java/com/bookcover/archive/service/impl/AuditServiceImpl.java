package com.bookcover.archive.service.impl;

import com.bookcover.archive.domain.audit.AuditAction;
import com.bookcover.archive.domain.audit.AuditEntityType;
import com.bookcover.archive.domain.audit.AuditLog;
import com.bookcover.archive.domain.user.User;
import com.bookcover.archive.exception.ResourceNotFoundException;
import com.bookcover.archive.repository.AuditLogRepository;
import com.bookcover.archive.repository.UserRepository;
import com.bookcover.archive.service.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * 감사 로그 서비스 구현체.
 * 모든 데이터 변경 이력을 추적하고 조회하는 기능을 구현합니다.
 *
 * Requirements: 16.1, 16.2, 16.5, 16.6
 */
@Service
@Transactional
public class AuditServiceImpl implements AuditService {

    private static final int DEFAULT_RETENTION_DAYS = 365; // 1년

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditServiceImpl(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Override
    public AuditLog log(AuditAction action, AuditEntityType entityType, UUID entityId,
                        UUID performedById, String previousValue, String newValue) {
        User performedBy = userRepository.findById(performedById)
                .orElseThrow(() -> new ResourceNotFoundException("User", performedById));

        AuditLog auditLog = AuditLog.create(action, entityType, entityId, performedBy, previousValue, newValue);
        return auditLogRepository.save(auditLog);
    }

    @Override
    public AuditLog logCreate(AuditEntityType entityType, UUID entityId, UUID performedById, String newValue) {
        return log(AuditAction.CREATE, entityType, entityId, performedById, null, newValue);
    }

    @Override
    public AuditLog logUpdate(AuditEntityType entityType, UUID entityId, UUID performedById,
                              String previousValue, String newValue) {
        return log(AuditAction.UPDATE, entityType, entityId, performedById, previousValue, newValue);
    }

    @Override
    public AuditLog logDelete(AuditEntityType entityType, UUID entityId, UUID performedById, String previousValue) {
        return log(AuditAction.DELETE, entityType, entityId, performedById, previousValue, null);
    }

    @Override
    public AuditLog logStatusChange(AuditEntityType entityType, UUID entityId, UUID performedById,
                                    String previousStatus, String newStatus) {
        String previousValue = String.format("{\"status\":\"%s\"}", previousStatus);
        String newValue = String.format("{\"status\":\"%s\"}", newStatus);
        return log(AuditAction.STATUS_CHANGE, entityType, entityId, performedById, previousValue, newValue);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getEntityHistory(AuditEntityType entityType, UUID entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> getEntityHistory(AuditEntityType entityType, UUID entityId, Pageable pageable) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> getUserActions(UUID userId, Pageable pageable) {
        return auditLogRepository.findByPerformedById(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> getLogsByAction(AuditAction action, Pageable pageable) {
        return auditLogRepository.findByAction(action, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> getLogsByDateRange(Instant start, Instant end, Pageable pageable) {
        return auditLogRepository.findByCreatedAtBetween(start, end, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> getRecentLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Override
    public long cleanupExpiredLogs(int retentionDays) {
        int days = retentionDays > 0 ? retentionDays : DEFAULT_RETENTION_DAYS;
        Instant cutoffDate = Instant.now().minus(days, ChronoUnit.DAYS);

        // Count before deletion for return value
        long countBefore = auditLogRepository.count();
        auditLogRepository.deleteByCreatedAtBefore(cutoffDate);
        long countAfter = auditLogRepository.count();

        return countBefore - countAfter;
    }
}
