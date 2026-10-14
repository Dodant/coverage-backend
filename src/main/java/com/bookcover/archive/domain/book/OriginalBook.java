package com.bookcover.archive.domain.book;

import com.bookcover.archive.domain.contributor.Author;
import com.bookcover.archive.domain.contributor.Designer;
import com.bookcover.archive.domain.contributor.Publisher;
import com.bookcover.archive.domain.media.Media;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * 원서 엔티티.
 * Media를 상속하며, 원서 고유의 필드를 포함합니다.
 *
 * Requirements:
 * - 1.1: 책 제목, 원서 제목, 나라, 언어, 연도, 작가, 출판사, 장르, 앞면 북커버를 필수 항목으로 검증
 * - 1.5: 원서가 성공적으로 등록되면 고유 식별자를 생성하고 데이터베이스에 저장
 * - 1.6: 북커버 디자이너 정보가 제공되면 디자이너 정보를 원서와 연결하여 저장
 * - 1.7: 에디션 정보가 제공되면 에디션 정보를 원서와 연결하여 저장
 */
@Entity
@Table(name = "original_books")
@DiscriminatorValue("BOOK_ORIGINAL")
public class OriginalBook extends Media {

    /** 원서 제목 (원어 제목) */
    @Column(nullable = false)
    private String originalTitle;

    /** 장르 */
    @Column(nullable = false)
    private String genre;

    /** ISBN (고유) */
    @Column(unique = true)
    private String isbn;

    /** 작가 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private Author author;

    /** 출판사 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publisher_id")
    private Publisher publisher;

    /** 북커버 디자이너 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "designer_id")
    private Designer designer;

    /** 에디션 정보 (예: 초판, 개정판, 특별판) */
    private String edition;

    /** 작품 그룹 (같은 작품의 여러 에디션 그룹화) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_id")
    private Work work;

    /** 번역서 목록 */
    @OneToMany(mappedBy = "originalBook", cascade = CascadeType.ALL)
    private List<TranslatedBook> translations = new ArrayList<>();

    /** 앞면 북커버 (필수) */
    @OneToOne(cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "front_cover_id", nullable = false)
    private BookCover frontCover;

    /** 뒷면 북커버 (선택) */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "back_cover_id")
    private BookCover backCover;

    // Getters and Setters

    public String getOriginalTitle() {
        return originalTitle;
    }

    public void setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }

    public Publisher getPublisher() {
        return publisher;
    }

    public void setPublisher(Publisher publisher) {
        this.publisher = publisher;
    }

    public Designer getDesigner() {
        return designer;
    }

    public void setDesigner(Designer designer) {
        this.designer = designer;
    }

    public String getEdition() {
        return edition;
    }

    public void setEdition(String edition) {
        this.edition = edition;
    }

    public Work getWork() {
        return work;
    }

    public void setWork(Work work) {
        this.work = work;
    }

    public List<TranslatedBook> getTranslations() {
        return translations;
    }

    public void setTranslations(List<TranslatedBook> translations) {
        this.translations = translations;
    }

    /**
     * 번역서를 추가합니다.
     * @param translatedBook 추가할 번역서
     */
    public void addTranslation(TranslatedBook translatedBook) {
        translations.add(translatedBook);
        translatedBook.setOriginalBook(this);
    }

    /**
     * 번역서를 제거합니다.
     * @param translatedBook 제거할 번역서
     */
    public void removeTranslation(TranslatedBook translatedBook) {
        translations.remove(translatedBook);
        translatedBook.setOriginalBook(null);
    }

    public BookCover getFrontCover() {
        return frontCover;
    }

    public void setFrontCover(BookCover frontCover) {
        this.frontCover = frontCover;
    }

    public BookCover getBackCover() {
        return backCover;
    }

    public void setBackCover(BookCover backCover) {
        this.backCover = backCover;
    }
}
