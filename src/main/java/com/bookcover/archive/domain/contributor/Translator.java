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
 * 옮긴이(번역가) 엔티티.
 * 번역가 정보를 관리하며, 별칭(alias) 지원을 포함합니다.
 */
@Entity
@Table(name = "translators")
public class Translator extends BaseEntity {

    /** 옮긴이 이름 (대표 이름) */
    @Column(nullable = false)
    private String name;

    /** 옮긴이 별칭 목록 (다른 표기, 필명 등) */
    @ElementCollection
    @CollectionTable(name = "translator_aliases", joinColumns = @JoinColumn(name = "translator_id"))
    @Column(name = "alias")
    private Set<String> aliases = new HashSet<>();

    /** 옮긴이 소개 */
    @Column(length = 2000)
    private String biography;

    /** 웹사이트 URL */
    private String websiteUrl;

    /** 국적 (ISO 3166-1 국가 코드) */
    @Column(length = 2)
    private String nationality;

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

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }
}
