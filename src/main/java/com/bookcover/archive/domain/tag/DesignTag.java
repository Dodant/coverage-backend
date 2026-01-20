package com.bookcover.archive.domain.tag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * 디자인 태그 엔티티.
 * 북커버에 부여할 수 있는 디자인 속성 태그를 관리합니다.
 * 카테고리별로 분류되며, 관리자 승인 정책을 지원합니다.
 *
 * Requirements: 20.1, 20.4
 */
@Entity
@Table(name = "design_tags")
public class DesignTag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TagCategory category;

    @Column(nullable = false)
    private Integer usageCount = 0;

    @Column(nullable = false)
    private Boolean approved = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TagCategory getCategory() {
        return category;
    }

    public void setCategory(TagCategory category) {
        this.category = category;
    }

    public Integer getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }

    public Boolean getApproved() {
        return approved;
    }

    public void setApproved(Boolean approved) {
        this.approved = approved;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 태그 사용 횟수를 증가시킵니다.
     */
    public void incrementUsageCount() {
        this.usageCount++;
    }

    /**
     * 태그 사용 횟수를 감소시킵니다.
     */
    public void decrementUsageCount() {
        if (this.usageCount > 0) {
            this.usageCount--;
        }
    }
}
