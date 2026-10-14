package com.bookcover.archive.domain.media;

import com.bookcover.archive.domain.book.BookCover;
import com.bookcover.archive.domain.common.ContentStatus;
import com.bookcover.archive.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 미디어 추상 베이스 클래스.
 * 북커버 외에 영화 포스터 등 다른 미디어 유형으로 확장 가능하도록 설계되었습니다.
 *
 * @see com.bookcover.archive.domain.book.OriginalBook
 * @see com.bookcover.archive.domain.book.TranslatedBook
 */
@Entity
@Table(name = "media")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "media_type")
public abstract class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** 미디어 제목 (각 국가/언어별 제목) */
    @Column(nullable = false)
    private String title;

    /** ISO 3166-1 국가 코드 */
    @Column(nullable = false, length = 2)
    private String countryCode;

    /** ISO 639-1 언어 코드 */
    @Column(nullable = false, length = 2)
    private String languageCode;

    /** 출판/발매 연도 (YYYY 형식) */
    @Column(nullable = false)
    private Integer year;

    /** 콘텐츠 상태 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContentStatus status = ContentStatus.PUBLISHED;

    /** 등록한 회원 */
    @ManyToOne(fetch = FetchType.LAZY)
    private User createdBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    /** 같은 책(ISBN)의 앞면 표지 변형들. 대표 표지는 getFrontCover() */
    @OneToMany(mappedBy = "book")
    @OrderBy("usedFrom ASC, createdAt ASC")
    private List<BookCover> covers = new ArrayList<>();

    public abstract BookCover getFrontCover();

    public abstract void setFrontCover(BookCover frontCover);

    public List<BookCover> getCovers() {
        return covers;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public ContentStatus getStatus() {
        return status;
    }

    public void setStatus(ContentStatus status) {
        this.status = status;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
