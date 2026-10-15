package com.bookcover.archive.service.impl;

import com.bookcover.archive.domain.book.BookCover;
import com.bookcover.archive.domain.book.OriginalBook;
import com.bookcover.archive.domain.book.Redesign;
import com.bookcover.archive.domain.book.TranslatedBook;
import com.bookcover.archive.domain.common.ContentStatus;
import com.bookcover.archive.domain.media.Media;
import com.bookcover.archive.exception.SearchException;
import com.bookcover.archive.repository.OriginalBookRepository;
import com.bookcover.archive.repository.RedesignRepository;
import com.bookcover.archive.repository.TranslatedBookRepository;
import com.bookcover.archive.search.document.BookCoverDocument;
import com.bookcover.archive.search.document.DocumentType;
import com.bookcover.archive.search.repository.BookCoverSearchRepository;
import com.bookcover.archive.service.SearchService;
import com.bookcover.archive.service.dto.SearchFacets;
import com.bookcover.archive.service.dto.SearchFilters;
import com.bookcover.archive.service.dto.SearchResult;
import com.bookcover.archive.service.dto.SearchResultItem;
import com.bookcover.archive.service.dto.SortOption;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * 검색 서비스 구현체.
 * Elasticsearch를 사용하여 전문 검색, 필터링, 정렬 및 브라우징 기능을 제공합니다.
 *
 * Requirements:
 * - 8.1: 책 제목, 작가, 출판사, 디자이너 이름, ISBN으로 검색 결과를 반환
 * - 8.2: 나라, 언어, 연도, 출판사, 장르별로 결과를 필터링
 * - 8.3: 검색 결과를 좋아요 수, 등록일, 연도순으로 정렬
 */
@Service
@Transactional(readOnly = true)
public class SearchServiceImpl implements SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchServiceImpl.class);
    private static final String PUBLISHED_STATUS = ContentStatus.PUBLISHED.name();

    private final ElasticsearchOperations elasticsearchOperations;
    private final BookCoverSearchRepository bookCoverSearchRepository;
    private final OriginalBookRepository originalBookRepository;
    private final TranslatedBookRepository translatedBookRepository;
    private final RedesignRepository redesignRepository;

    public SearchServiceImpl(
            ElasticsearchOperations elasticsearchOperations,
            BookCoverSearchRepository bookCoverSearchRepository,
            OriginalBookRepository originalBookRepository,
            TranslatedBookRepository translatedBookRepository,
            RedesignRepository redesignRepository) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.bookCoverSearchRepository = bookCoverSearchRepository;
        this.originalBookRepository = originalBookRepository;
        this.translatedBookRepository = translatedBookRepository;
        this.redesignRepository = redesignRepository;
    }

    @Override
    public SearchResult search(String query, SearchFilters filters, Pageable pageable) {
        if (query == null || query.isBlank()) {
            return SearchResult.empty();
        }

        try {
            NativeQuery searchQuery = buildSearchQuery(query, filters, pageable);
            SearchHits<BookCoverDocument> searchHits = elasticsearchOperations.search(
                    searchQuery, BookCoverDocument.class);

            List<SearchResultItem> items = searchHits.getSearchHits().stream()
                    .map(this::toSearchResultItem)
                    .toList();

            int totalPages = (int) Math.ceil((double) searchHits.getTotalHits() / pageable.getPageSize());

            return new SearchResult(
                    items,
                    searchHits.getTotalHits(),
                    SearchFacets.empty(),
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    totalPages
            );
        } catch (Exception e) {
            log.error("Search failed for query: {}", query, e);
            throw new SearchException(e);
        }
    }

    private NativeQuery buildSearchQuery(String query, SearchFilters filters, Pageable pageable) {
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        // Multi-match query for full-text search
        boolQueryBuilder.must(Query.of(q -> q
                .multiMatch(mm -> mm
                        .query(query)
                        .fields("title^3", "originalTitle^2", "author^2", "publisher", "designers", "isbn^4", "description")
                        .fuzziness("AUTO")
                )
        ));

        // Status filter - only show published content
        boolQueryBuilder.filter(Query.of(q -> q
                .term(t -> t.field("status").value(PUBLISHED_STATUS))
        ));

        // Apply filters
        applyFilters(boolQueryBuilder, filters);

        // Build query with sorting
        SortOption sortOption = filters != null && filters.sortBy() != null ? filters.sortBy() : SortOption.RELEVANCE;
        Sort sort = getSortForOption(sortOption);

        return NativeQuery.builder()
                .withQuery(Query.of(q -> q.bool(boolQueryBuilder.build())))
                .withPageable(pageable)
                .withSort(sort)
                .build();
    }

    private void applyFilters(BoolQuery.Builder boolQueryBuilder, SearchFilters filters) {
        if (filters == null) {
            return;
        }

        if (filters.countryCode() != null && !filters.countryCode().isBlank()) {
            boolQueryBuilder.filter(Query.of(q -> q
                    .term(t -> t.field("countryCode").value(filters.countryCode()))
            ));
        }

        if (filters.languageCode() != null && !filters.languageCode().isBlank()) {
            boolQueryBuilder.filter(Query.of(q -> q
                    .term(t -> t.field("languageCode").value(filters.languageCode()))
            ));
        }

        if (filters.genre() != null && !filters.genre().isBlank()) {
            boolQueryBuilder.filter(Query.of(q -> q
                    .term(t -> t.field("genre").value(filters.genre()))
            ));
        }

        if (filters.publisherId() != null) {
            boolQueryBuilder.filter(Query.of(q -> q
                    .term(t -> t.field("publisherId").value(filters.publisherId().toString()))
            ));
        }

        if (filters.yearFrom() != null || filters.yearTo() != null) {
            Integer from = filters.yearFrom();
            Integer to = filters.yearTo();
            boolQueryBuilder.filter(Query.of(q -> q
                    .range(r -> {
                        r.field("year");
                        if (from != null) r.gte(co.elastic.clients.json.JsonData.of(from));
                        if (to != null) r.lte(co.elastic.clients.json.JsonData.of(to));
                        return r;
                    })
            ));
        }

        if (filters.tagIds() != null && !filters.tagIds().isEmpty()) {
            List<String> tagIdStrings = filters.tagIds().stream()
                    .map(UUID::toString)
                    .toList();
            boolQueryBuilder.filter(Query.of(q -> q
                    .terms(t -> t
                            .field("tagIds")
                            .terms(tv -> tv.value(tagIdStrings.stream()
                                    .map(co.elastic.clients.elasticsearch._types.FieldValue::of)
                                    .toList()))
                    )
            ));
        }
    }

    private Sort getSortForOption(SortOption sortOption) {
        return switch (sortOption) {
            case LIKES -> Sort.by(Sort.Direction.DESC, "likeCount");
            case NEWEST -> Sort.by(Sort.Direction.DESC, "createdAt");
            case YEAR_ASC -> Sort.by(Sort.Direction.ASC, "year");
            case YEAR_DESC -> Sort.by(Sort.Direction.DESC, "year");
            case RELEVANCE -> Sort.unsorted();
        };
    }

    private SearchResultItem toSearchResultItem(SearchHit<BookCoverDocument> hit) {
        BookCoverDocument doc = hit.getContent();
        return new SearchResultItem(
                doc.getId(),
                DocumentType.valueOf(doc.getType()),
                doc.getTitle(),
                doc.getThumbnailUrl(),
                doc.getAuthor(),
                doc.getPublisher(),
                doc.getDesigners(),
                doc.getYear(),
                doc.getCountryCode(),
                doc.getGenre(),
                doc.getLikeCount(),
                doc.getCreatedAt()
        );
    }

    @Override
    public Page<BookCoverDocument> browseLatest(Pageable pageable) {
        Pageable latestPage = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        try {
            return bookCoverSearchRepository.findByStatus(PUBLISHED_STATUS, latestPage);
        } catch (Exception e) {
            log.error("최신 북커버 조회에 실패했습니다", e);
            throw new SearchException(e);
        }
    }

    @Override
    public Page<BookCoverDocument> browseByCountry(String countryCode, Pageable pageable) {
        return bookCoverSearchRepository.findByCountryCodeAndStatus(countryCode, PUBLISHED_STATUS, pageable);
    }

    @Override
    public Page<BookCoverDocument> browseByYear(int year, Pageable pageable) {
        return bookCoverSearchRepository.findByYearAndStatus(year, PUBLISHED_STATUS, pageable);
    }

    @Override
    public Page<BookCoverDocument> browseByGenre(String genre, Pageable pageable) {
        return bookCoverSearchRepository.findByGenreAndStatus(genre, PUBLISHED_STATUS, pageable);
    }

    @Override
    public Page<BookCoverDocument> browseByTag(UUID tagId, Pageable pageable) {
        return bookCoverSearchRepository.findByTagIdsContainingAndStatus(tagId.toString(), PUBLISHED_STATUS, pageable);
    }

    @Override
    @Transactional
    public void reindexAll() {
        log.info("Starting full reindex...");

        bookCoverSearchRepository.deleteAll();

        originalBookRepository.findAll().forEach(book -> {
            BookCoverDocument doc = toDocument(book);
            bookCoverSearchRepository.save(doc);
        });

        translatedBookRepository.findAll().forEach(book -> {
            BookCoverDocument doc = toDocument(book);
            bookCoverSearchRepository.save(doc);
        });

        redesignRepository.findAll().forEach(redesign -> {
            BookCoverDocument doc = toDocument(redesign);
            bookCoverSearchRepository.save(doc);
        });

        log.info("Full reindex completed");
    }

    @Override
    @Transactional
    public void indexOriginalBook(UUID bookId) {
        originalBookRepository.findById(bookId).ifPresent(book -> {
            BookCoverDocument doc = toDocument(book);
            bookCoverSearchRepository.save(doc);
            log.debug("Indexed original book: {}", bookId);
        });
    }

    @Override
    @Transactional
    public void indexTranslatedBook(UUID bookId) {
        translatedBookRepository.findById(bookId).ifPresent(book -> {
            BookCoverDocument doc = toDocument(book);
            bookCoverSearchRepository.save(doc);
            log.debug("Indexed translated book: {}", bookId);
        });
    }

    @Override
    @Transactional
    public void indexRedesign(UUID redesignId) {
        redesignRepository.findById(redesignId).ifPresent(redesign -> {
            BookCoverDocument doc = toDocument(redesign);
            bookCoverSearchRepository.save(doc);
            log.debug("Indexed redesign: {}", redesignId);
        });
    }

    @Override
    @Transactional
    public void removeFromIndex(String documentId) {
        bookCoverSearchRepository.deleteById(documentId);
        log.debug("Removed from index: {}", documentId);
    }

    @Override
    public long countByCountry(String countryCode) {
        return bookCoverSearchRepository.countByCountryCodeAndStatus(countryCode, PUBLISHED_STATUS);
    }

    @Override
    public long countByYear(int year) {
        return bookCoverSearchRepository.countByYearAndStatus(year, PUBLISHED_STATUS);
    }

    @Override
    public long countByGenre(String genre) {
        return bookCoverSearchRepository.countByGenreAndStatus(genre, PUBLISHED_STATUS);
    }


    private BookCoverDocument toDocument(OriginalBook book) {
        BookCoverDocument doc = new BookCoverDocument();
        doc.setId("original_" + book.getId().toString());
        doc.setType(DocumentType.ORIGINAL.name());
        doc.setTitle(book.getTitle());
        doc.setOriginalTitle(book.getOriginalTitle());
        doc.setIsbn(book.getIsbn());
        doc.setCountryCode(book.getCountryCode());
        doc.setLanguageCode(book.getLanguageCode());
        doc.setYear(book.getYear());
        doc.setGenre(book.getGenre());
        doc.setStatus(book.getStatus().name());
        doc.setCreatedAt(book.getCreatedAt());

        if (book.getAuthor() != null) {
            doc.setAuthor(book.getAuthor().getName());
            doc.setAuthorId(book.getAuthor().getId().toString());
        }

        if (book.getPublisher() != null) {
            doc.setPublisher(book.getPublisher().getName());
            doc.setPublisherId(book.getPublisher().getId().toString());
        }

        setCredits(doc, book);
        setTags(doc, book);

        if (book.getFrontCover() != null) {
            doc.setThumbnailUrl(book.getFrontCover().getThumbnailUrl());
            doc.setLikeCount(book.getFrontCover().getLikeCount());
        }

        return doc;
    }

    private BookCoverDocument toDocument(TranslatedBook book) {
        BookCoverDocument doc = new BookCoverDocument();
        doc.setId("translated_" + book.getId().toString());
        doc.setType(DocumentType.TRANSLATED.name());
        doc.setTitle(book.getTitle());
        doc.setIsbn(book.getIsbn());
        doc.setCountryCode(book.getCountryCode());
        doc.setLanguageCode(book.getLanguageCode());
        doc.setYear(book.getYear());
        doc.setStatus(book.getStatus().name());
        doc.setCreatedAt(book.getCreatedAt());

        if (book.getOriginalBook() != null) {
            doc.setGenre(book.getOriginalBook().getGenre());
            doc.setOriginalTitle(book.getOriginalBook().getOriginalTitle());
        }

        if (book.getAuthor() != null) {
            doc.setAuthor(book.getAuthor().getName());
            doc.setAuthorId(book.getAuthor().getId().toString());
        }

        if (book.getPublisher() != null) {
            doc.setPublisher(book.getPublisher().getName());
            doc.setPublisherId(book.getPublisher().getId().toString());
        }

        setCredits(doc, book);
        setTags(doc, book);

        if (book.getFrontCover() != null) {
            doc.setThumbnailUrl(book.getFrontCover().getThumbnailUrl());
            doc.setLikeCount(book.getFrontCover().getLikeCount());
        }

        return doc;
    }

    /** 앞면 표지 변형과 뒷면 표지의 크레딧 기여자를 중복 없이 모은다 */
    private void setCredits(BookCoverDocument doc, Media book) {
        Map<String, String> designers = new LinkedHashMap<>();
        Stream.concat(book.getCovers().stream(), Stream.ofNullable(backCoverOf(book)))
                .flatMap(cover -> cover.getCredits().stream())
                .forEach(c -> designers.putIfAbsent(c.getDesigner().getId().toString(), c.getDesigner().getName()));
        doc.setDesignerIds(new ArrayList<>(designers.keySet()));
        doc.setDesigners(new ArrayList<>(designers.values()));
    }

    /** Requirements: 20.1, 20.3 - 표지의 승인된 태그를 검색 문서에 모은다. */
    private void setTags(BookCoverDocument doc, Media book) {
        Map<String, String> tags = new LinkedHashMap<>();
        Stream.concat(Stream.concat(book.getCovers().stream(), Stream.ofNullable(book.getFrontCover())),
                        Stream.ofNullable(backCoverOf(book))).distinct()
                .filter(cover -> !cover.isHidden())
                .flatMap(cover -> cover.getTags().stream())
                .filter(tag -> Boolean.TRUE.equals(tag.getApproved()))
                .forEach(tag -> tags.putIfAbsent(tag.getId().toString(), tag.getName()));
        doc.setTagIds(new ArrayList<>(tags.keySet()));
        doc.setTags(new ArrayList<>(tags.values()));
    }

    private static BookCover backCoverOf(Media book) {
        if (book instanceof OriginalBook ob) return ob.getBackCover();
        if (book instanceof TranslatedBook tb) return tb.getBackCover();
        return null;
    }

    private BookCoverDocument toDocument(Redesign redesign) {
        BookCoverDocument doc = new BookCoverDocument();
        doc.setId("redesign_" + redesign.getId().toString());
        doc.setType(DocumentType.REDESIGN.name());
        doc.setDescription(redesign.getDescription());
        doc.setThumbnailUrl(redesign.getThumbnailUrl());
        doc.setLikeCount(redesign.getLikeCount());
        doc.setStatus(redesign.getStatus().name());
        doc.setCreatedAt(redesign.getCreatedAt());

        if (redesign.getCreatedBy() != null) {
            doc.setCreatorName(redesign.getCreatedBy().getDisplayName());
            doc.setCreatorId(redesign.getCreatedBy().getId().toString());
        }

        if (redesign.getOriginalBook() != null) {
            OriginalBook original = redesign.getOriginalBook();
            doc.setTitle(original.getTitle());
            doc.setOriginalTitle(original.getOriginalTitle());
            doc.setCountryCode(original.getCountryCode());
            doc.setLanguageCode(original.getLanguageCode());
            doc.setYear(original.getYear());
            doc.setGenre(original.getGenre());

            if (original.getAuthor() != null) {
                doc.setAuthor(original.getAuthor().getName());
                doc.setAuthorId(original.getAuthor().getId().toString());
            }
        } else if (redesign.getTranslatedBook() != null) {
            TranslatedBook translated = redesign.getTranslatedBook();
            doc.setTitle(translated.getTitle());
            doc.setCountryCode(translated.getCountryCode());
            doc.setLanguageCode(translated.getLanguageCode());
            doc.setYear(translated.getYear());

            if (translated.getOriginalBook() != null) {
                doc.setGenre(translated.getOriginalBook().getGenre());
                doc.setOriginalTitle(translated.getOriginalBook().getOriginalTitle());
            }

            if (translated.getAuthor() != null) {
                doc.setAuthor(translated.getAuthor().getName());
                doc.setAuthorId(translated.getAuthor().getId().toString());
            }
        }

        return doc;
    }
}
