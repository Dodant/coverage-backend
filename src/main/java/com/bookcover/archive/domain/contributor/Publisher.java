package com.bookcover.archive.domain.contributor;

import com.bookcover.archive.domain.common.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

/**
 * 출판사 엔티티.
 * 출판사 정보를 관리하며, 별칭(alias) 지원을 포함합니다.
 */
@Entity
@Table(name = "publishers")
public class Publisher extends BaseEntity {

    /** 출판사 이름 (대표 이름) */
    @Column(nullable = false)
    private String name;

    /** 출판사 별칭 목록 (다른 표기, 이전 이름 등) */
    @ElementCollection
    @CollectionTable(name = "publisher_aliases", joinColumns = @JoinColumn(name = "publisher_id"))
    @Column(name = "alias")
    private Set<String> aliases = new HashSet<>();

    /** 출판사 소개 */
    @Column(length = 2000)
    private String description;

    /** 웹사이트 URL */
    private String websiteUrl;

    /** 소재 국가 (ISO 3166-1 국가 코드) */
    @Column(length = 2)
    private String countryCode;

    // Getters and Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getAliases() {
        return aliases;
    }

    public void setAliases(Set<String> aliases) {
        this.aliases = aliases;
    }

    public void addAlias(String alias) {
        this.aliases.add(alias);
    }

    public void removeAlias(String alias) {
        this.aliases.remove(alias);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }
}
