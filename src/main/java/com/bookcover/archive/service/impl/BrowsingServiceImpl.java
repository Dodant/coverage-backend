package com.bookcover.archive.service.impl;

import com.bookcover.archive.domain.common.ContentStatus;
import com.bookcover.archive.repository.DesignTagRepository;
import com.bookcover.archive.search.document.BookCoverDocument;
import com.bookcover.archive.search.document.DocumentType;
import com.bookcover.archive.search.repository.BookCoverSearchRepository;
import com.bookcover.archive.service.BrowsingService;
import com.bookcover.archive.service.dto.BrowseResult;
import com.bookcover.archive.service.dto.BrowseResultItem;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 브라우징 서비스 구현체.
 * 국가별, 연도별, 장르별, 태그별 브라우징 기능을 제공합니다.
 *
 * Requirements:
 * - 8.5: 국가별 브라우징 페이지를 제공하여 특정 국가의 모든 북커버를 조회
 * - 8.6: 연도별 브라우징 페이지를 제공하여 특정 연도의 모든 북커버를 조회
 * - 8.7: 장르별 브라우징 페이지를 제공하여 특정 장르의 모든 북커버를 조회
 * - 20.6: 태그별 브라우징 페이지를 제공
 */
@Service
@Transactional(readOnly = true)
public class BrowsingServiceImpl implements BrowsingService {

    private static final Logger log = LoggerFactory.getLogger(BrowsingServiceImpl.class);
    private static final String PUBLISHED_STATUS = ContentStatus.PUBLISHED.name();

    private final BookCoverSearchRepository bookCoverSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final DesignTagRepository designTagRepository;

    public BrowsingServiceImpl(
            BookCoverSearchRepository bookCoverSearchRepository,
            ElasticsearchOperations elasticsearchOperations,
            DesignTagRepository designTagRepository) {
        this.bookCoverSearchRepository = bookCoverSearchRepository;
        this.elasticsearchOperations = elasticsearchOperations;
        this.designTagRepository = designTagRepository;
    }

    @Override
    public BrowseResult browseByCountry(String countryCode, Pageable pageable) {
        Page<BookCoverDocument> page = bookCoverSearchRepository.findByCountryCodeAndStatus(
                countryCode, PUBLISHED_STATUS, pageable);
        return toBrowseResult("country", countryCode, page, pageable);
    }

    @Override
    public BrowseResult browseByYear(int year, Pageable pageable) {
        Page<BookCoverDocument> page = bookCoverSearchRepository.findByYearAndStatus(
                year, PUBLISHED_STATUS, pageable);
        return toBrowseResult("year", String.valueOf(year), page, pageable);
    }

    @Override
    public BrowseResult browseByGenre(String genre, Pageable pageable) {
        Page<BookCoverDocument> page = bookCoverSearchRepository.findByGenreAndStatus(
                genre, PUBLISHED_STATUS, pageable);
        return toBrowseResult("genre", genre, page, pageable);
    }

    @Override
    public BrowseResult browseByTag(UUID tagId, Pageable pageable) {
        Page<BookCoverDocument> page = bookCoverSearchRepository.findByTagIdsContainingAndStatus(
                tagId.toString(), PUBLISHED_STATUS, pageable);
        return toBrowseResult("tag", tagId.toString(), page, pageable);
    }

    @Override
    public Map<String, Long> getCountryCounts() {
        return getAggregationCounts("countryCode");
    }

    @Override
    public Map<Integer, Long> getYearCounts() {
        Map<String, Long> stringCounts = getAggregationCounts("year");
        Map<Integer, Long> yearCounts = new HashMap<>();
        stringCounts.forEach((key, value) -> {
            try {
                yearCounts.put(Integer.parseInt(key), value);
            } catch (NumberFormatException e) {
                log.warn("Invalid year value in aggregation: {}", key);
            }
        });
        return yearCounts;
    }

    @Override
    public Map<String, Long> getGenreCounts() {
        return getAggregationCounts("genre");
    }

    @Override
    public List<TagInfo> getPopularTags(int limit) {
        return designTagRepository.findByApprovedTrueOrderByUsageCountDesc()
                .stream()
                .limit(limit)
                .map(tag -> new TagInfo(
                        tag.getId(),
                        tag.getName(),
                        tag.getCategory().name(),
                        tag.getUsageCount()
                ))
                .toList();
    }

    private BrowseResult toBrowseResult(String category, String categoryValue,
                                         Page<BookCoverDocument> page, Pageable pageable) {
        List<BrowseResultItem> items = page.getContent().stream()
                .map(this::toBrowseResultItem)
                .toList();

        return new BrowseResult(
                category,
                categoryValue,
                page.getTotalElements(),
                items,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                page.getTotalPages()
        );
    }

    private BrowseResultItem toBrowseResultItem(BookCoverDocument doc) {
        return new BrowseResultItem(
                doc.getId(),
                DocumentType.valueOf(doc.getType()),
                doc.getTitle(),
                doc.getThumbnailUrl(),
                doc.getAuthor(),
                doc.getPublisher(),
                doc.getYear(),
                doc.getCountryCode(),
                doc.getGenre(),
                doc.getLikeCount(),
                doc.getCreatedAt()
        );
    }

    private Map<String, Long> getAggregationCounts(String field) {
        try {
            NativeQuery query = NativeQuery.builder()
                    .withQuery(Query.of(q -> q
                            .term(t -> t.field("status").value(PUBLISHED_STATUS))
                    ))
                    .withAggregation(field + "_agg", Aggregation.of(a -> a
                            .terms(t -> t.field(field).size(1000))
                    ))
                    .withMaxResults(0)
                    .build();

            SearchHits<BookCoverDocument> searchHits = elasticsearchOperations.search(
                    query, BookCoverDocument.class);

            Map<String, Long> counts = new HashMap<>();

            if (searchHits.hasAggregations()) {
                ElasticsearchAggregations aggregations =
                        (ElasticsearchAggregations) searchHits.getAggregations();
                if (aggregations != null) {
                    var termsAgg = aggregations.aggregations().get(0).aggregation()
                            .getAggregate().sterms();
                    if (termsAgg != null) {
                        for (StringTermsBucket bucket : termsAgg.buckets().array()) {
                            counts.put(bucket.key().stringValue(), bucket.docCount());
                        }
                    }
                }
            }

            return counts;
        } catch (Exception e) {
            log.error("Failed to get aggregation counts for field: {}", field, e);
            return Map.of();
        }
    }
}
