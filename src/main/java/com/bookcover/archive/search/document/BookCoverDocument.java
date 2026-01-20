package com.bookcover.archive.search.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.time.Instant;
import java.util.List;

/**
 * Elasticsearch 북커버 검색 문서.
 * 원서, 번역서, 리디자인을 통합하여 검색할 수 있는 인덱스 문서입니다.
 *
 * Requirements:
 * - 8.1: 책 제목, 작가, 출판사, 디자이너 이름, ISBN으로 검색 결과를 반환
 * - 8.2: 나라, 언어, 연도, 출판사, 장르별로 결과를 필터링
 * - 8.3: 검색 결과를 좋아요 수, 등록일, 연도순으로 정렬
 */
@Document(indexName = "book_covers")
@Setting(settingPath = "elasticsearch/book-covers-settings.json")
public class BookCoverDocument {

    @Id
    private String id;

    /** 문서 유형: ORIGINAL, TRANSLATED, REDESIGN */
    @Field(type = FieldType.Keyword)
    private String type;

    /** 책 제목 (한국어 분석기 적용) */
    @Field(type = FieldType.Text, analyzer = "korean_analyzer", searchAnalyzer = "korean_analyzer")
    private String title;

    /** 원서 제목 (표준 분석기 적용) */
    @Field(type = FieldType.Text, analyzer = "standard")
    private String originalTitle;

    /** 작가 이름 (한국어 분석기 적용) */
    @Field(type = FieldType.Text, analyzer = "korean_analyzer", searchAnalyzer = "korean_analyzer")
    private String author;

    /** 작가 ID */
    @Field(type = FieldType.Keyword)
    private String authorId;

    /** 출판사 이름 (한국어 분석기 적용) */
    @Field(type = FieldType.Text, analyzer = "korean_analyzer", searchAnalyzer = "korean_analyzer")
    private String publisher;

    /** 출판사 ID */
    @Field(type = FieldType.Keyword)
    private String publisherId;

    /** 디자이너 이름 (한국어 분석기 적용) */
    @Field(type = FieldType.Text, analyzer = "korean_analyzer", searchAnalyzer = "korean_analyzer")
    private String designer;

    /** 디자이너 ID */
    @Field(type = FieldType.Keyword)
    private String designerId;

    /** ISBN */
    @Field(type = FieldType.Keyword)
    private String isbn;

    /** 국가 코드 (ISO 3166-1) */
    @Field(type = FieldType.Keyword)
    private String countryCode;

    /** 언어 코드 (ISO 639-1) */
    @Field(type = FieldType.Keyword)
    private String languageCode;

    /** 출판 연도 */
    @Field(type = FieldType.Integer)
    private Integer year;

    /** 장르 */
    @Field(type = FieldType.Keyword)
    private String genre;

    /** 디자인 태그 목록 */
    @Field(type = FieldType.Keyword)
    private List<String> tags;

    /** 태그 ID 목록 */
    @Field(type = FieldType.Keyword)
    private List<String> tagIds;

    /** 좋아요 수 */
    @Field(type = FieldType.Integer)
    private Integer likeCount;

    /** 썸네일 URL */
    @Field(type = FieldType.Keyword, index = false)
    private String thumbnailUrl;

    /** 생성일시 */
    @Field(type = FieldType.Date)
    private Instant createdAt;

    /** 콘텐츠 상태 */
    @Field(type = FieldType.Keyword)
    private String status;

    // 리디자인 전용 필드
    /** 리디자인 설명 */
    @Field(type = FieldType.Text, analyzer = "korean_analyzer", searchAnalyzer = "korean_analyzer")
    private String description;

    /** 리디자인 작성자 이름 */
    @Field(type = FieldType.Text, analyzer = "korean_analyzer", searchAnalyzer = "korean_analyzer")
    private String creatorName;

    /** 리디자인 작성자 ID */
    @Field(type = FieldType.Keyword)
    private String creatorId;

    // Constructors
    public BookCoverDocument() {
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

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

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getPublisherId() {
        return publisherId;
    }

    public void setPublisherId(String publisherId) {
        this.publisherId = publisherId;
    }

    public String getDesigner() {
        return designer;
    }

    public void setDesigner(String designer) {
        this.designer = designer;
    }

    public String getDesignerId() {
        return designerId;
    }

    public void setDesignerId(String designerId) {
        this.designerId = designerId;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
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

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getTagIds() {
        return tagIds;
    }

    public void setTagIds(List<String> tagIds) {
        this.tagIds = tagIds;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }
}
