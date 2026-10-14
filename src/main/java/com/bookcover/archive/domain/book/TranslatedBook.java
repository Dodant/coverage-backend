package com.bookcover.archive.domain.book;

import com.bookcover.archive.domain.contributor.Author;
import com.bookcover.archive.domain.contributor.Designer;
import com.bookcover.archive.domain.contributor.Publisher;
import com.bookcover.archive.domain.contributor.Translator;
import com.bookcover.archive.domain.media.Media;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * 번역서 엔티티.
 * Media를 상속하며, 원서와 연결되어 각국 번역판의 커버 정보를 포함합니다.
 *
 * Requirements:
 * - 2.1: 번역서를 등록하면 해당 번역서를 기존 원서와 연결
 * - 2.2: 각국 책 제목, 나라, 언어, 연도, 작가, 옮긴이, 출판사, 앞면 북커버를 필수 항목으로 검증
 * - 2.5: 에디션 정보가 제공되면 에디션 정보를 번역서와 연결하여 저장
 */
@Entity
@Table(name = "translated_books")
@DiscriminatorValue("BOOK_TRANSLATED")
public class TranslatedBook extends Media {

    /** ISBN (고유) */
    @Column(unique = true)
    private String isbn;

    /** 원서 (필수) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "original_book_id", nullable = false)
    private OriginalBook originalBook;

    /** 작가 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private Author author;

    /** 옮긴이 (번역가) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "translator_id")
    private Translator translator;

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

    /** 앞면 북커버 (필수) */
    @OneToOne(cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "front_cover_id", nullable = false)
    private BookCover frontCover;

    /** 뒷면 북커버 (선택) */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "back_cover_id")
    private BookCover backCover;

    // Getters and Setters

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public OriginalBook getOriginalBook() {
        return originalBook;
    }

    public void setOriginalBook(OriginalBook originalBook) {
        this.originalBook = originalBook;
    }

    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }

    public Translator getTranslator() {
        return translator;
    }

    public void setTranslator(Translator translator) {
        this.translator = translator;
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
