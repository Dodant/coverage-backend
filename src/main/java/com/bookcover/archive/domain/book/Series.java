package com.bookcover.archive.domain.book;

import com.bookcover.archive.domain.common.BaseEntity;
import com.bookcover.archive.domain.contributor.Publisher;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * 시리즈·총서 (예: 민음사 세계문학전집, 펭귄 클래식).
 * 같은 이름이라도 출판사가 다르면 다른 시리즈다.
 */
@Entity
@Table(name = "series")
public class Series extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publisher_id")
    private Publisher publisher;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Publisher getPublisher() {
        return publisher;
    }

    public void setPublisher(Publisher publisher) {
        this.publisher = publisher;
    }
}
