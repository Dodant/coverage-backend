package com.bookcover.archive.domain.takedown;

import com.bookcover.archive.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * 저작권 삭제 요청 엔티티.
 * 권리자(출판사/대리인)의 저작권 삭제 요청을 관리합니다.
 * 요청자 정보, 대상 URL, 사유, 증빙을 기록합니다.
 *
 * Requirements: 17.1, 17.2
 */
@Entity
@Table(name = "takedown_requests")
public class TakedownRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** 요청자 이름 (권리자 또는 대리인) */
    @Column(nullable = false)
    private String requesterName;

    /** 요청자 이메일 */
    @Column(nullable = false)
    private String requesterEmail;

    /** 요청자 소속 (출판사명 등) */
    private String requesterOrganization;

    /** 대상 콘텐츠 URL */
    @Column(nullable = false)
    private String targetUrl;

    /** 대상 콘텐츠 ID */
    @Column(name = "target_id")
    private UUID targetId;

    /** 삭제 요청 사유 */
    @Column(nullable = false, length = 2000)
    private String reason;

    /** 증빙 자료 URL (저작권 증명 문서 등) */
    private String evidenceUrl;

    /** 증빙 자료 설명 */
    @Column(length = 1000)
    private String evidenceDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TakedownStatus status = TakedownStatus.PENDING;

    /** 검토자 */
    @ManyToOne(fetch = FetchType.LAZY)
    private User reviewedBy;

    /** 검토 메모 */
    @Column(length = 1000)
    private String reviewNote;

    /** 검토 일시 */
    private Instant reviewedAt;

    /** 재업로드 방지 플래그 */
    @Column(nullable = false)
    private Boolean preventReupload = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }

    public String getRequesterEmail() {
        return requesterEmail;
    }

    public void setRequesterEmail(String requesterEmail) {
        this.requesterEmail = requesterEmail;
    }

    public String getRequesterOrganization() {
        return requesterOrganization;
    }

    public void setRequesterOrganization(String requesterOrganization) {
        this.requesterOrganization = requesterOrganization;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public void setTargetId(UUID targetId) {
        this.targetId = targetId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getEvidenceUrl() {
        return evidenceUrl;
    }

    public void setEvidenceUrl(String evidenceUrl) {
        this.evidenceUrl = evidenceUrl;
    }

    public String getEvidenceDescription() {
        return evidenceDescription;
    }

    public void setEvidenceDescription(String evidenceDescription) {
        this.evidenceDescription = evidenceDescription;
    }

    public TakedownStatus getStatus() {
        return status;
    }

    public void setStatus(TakedownStatus status) {
        this.status = status;
    }

    public User getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(User reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public void setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public Boolean getPreventReupload() {
        return preventReupload;
    }

    public void setPreventReupload(Boolean preventReupload) {
        this.preventReupload = preventReupload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 삭제 요청을 승인 처리합니다.
     * 콘텐츠를 비공개 처리하고 재업로드 방지 플래그를 설정합니다.
     *
     * @param reviewer 검토자
     * @param note 검토 메모
     */
    public void approve(User reviewer, String note) {
        this.reviewedBy = reviewer;
        this.status = TakedownStatus.APPROVED;
        this.reviewNote = note;
        this.reviewedAt = Instant.now();
        this.preventReupload = true;
    }

    /**
     * 삭제 요청을 거부 처리합니다.
     *
     * @param reviewer 검토자
     * @param note 거부 사유
     */
    public void reject(User reviewer, String note) {
        this.reviewedBy = reviewer;
        this.status = TakedownStatus.REJECTED;
        this.reviewNote = note;
        this.reviewedAt = Instant.now();
    }
}
