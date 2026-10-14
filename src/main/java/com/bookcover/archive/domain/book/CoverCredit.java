package com.bookcover.archive.domain.book;

import com.bookcover.archive.domain.contributor.Designer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

/**
 * 표지 크레딧. 한 표지에 역할별로 여러 기여자(디자이너·일러스트레이터·사진가 등)를 연결한다.
 * 리커버처럼 같은 책이라도 표지마다 크레딧이 다를 수 있어 책이 아닌 표지에 붙는다.
 *
 * Requirements: 1.6 - 북커버 디자이너 정보를 연결하여 저장
 */
@Entity
@Table(name = "cover_credits",
        uniqueConstraints = @UniqueConstraint(name = "uk_cover_credits_cover_designer_role",
                columnNames = {"cover_id", "designer_id", "role"}),
        indexes = @Index(name = "idx_cover_credits_designer", columnList = "designer_id"))
public class CoverCredit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cover_id", nullable = false)
    private BookCover cover;

    /** 기여자. 일러스트레이터·사진가도 Designer 엔티티로 관리한다 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "designer_id", nullable = false)
    private Designer designer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CreditRole role;

    protected CoverCredit() {
    }

    public CoverCredit(BookCover cover, Designer designer, CreditRole role) {
        this.cover = cover;
        this.designer = designer;
        this.role = role;
    }

    public UUID getId() {
        return id;
    }

    public BookCover getCover() {
        return cover;
    }

    public Designer getDesigner() {
        return designer;
    }

    public CreditRole getRole() {
        return role;
    }
}
