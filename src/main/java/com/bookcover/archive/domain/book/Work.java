package com.bookcover.archive.domain.book;

import com.bookcover.archive.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 작품 엔티티.
 * 같은 작품의 여러 에디션(원서, 번역서 등)을 그룹화하기 위한 엔티티입니다.
 * 예: "1984" 작품 아래에 여러 출판사의 원서와 각국 번역서가 연결됩니다.
 *
 * Requirements: 6.1 - 같은 원서에 대해 다른 출판사의 에디션을 등록하면
 * 각 에디션을 독립적인 엔트리로 저장하되 같은 작품으로 그룹화한다.
 */
@Entity
@Table(name = "works")
public class Work extends BaseEntity {

    /** 작품의 대표 제목 (원서 제목 기준) */
    @Column(nullable = false)
    private String title;

    /** 작품의 원어 제목 */
    private String originalTitle;

    /** 작품 설명 */
    @Column(length = 2000)
    private String description;

    // Getters and Setters

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public void setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
