package com.bookcover.archive.controller;

import com.bookcover.archive.search.document.BookCoverDocument;
import com.bookcover.archive.service.SearchService;
import com.bookcover.archive.service.dto.BrowseResult;
import com.bookcover.archive.service.dto.BrowseResultItem;
import com.bookcover.archive.service.dto.SearchFilters;
import com.bookcover.archive.service.dto.SearchResult;
import com.bookcover.archive.service.dto.SortOption;
import com.bookcover.archive.exception.ValidationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 검색 API 컨트롤러.
 * 검색, 필터링, 브라우징 엔드포인트를 제공합니다.
 *
 * Requirements: 8.1, 8.5, 8.6, 8.7
 */
@RestController
@RequestMapping("/search")
@Tag(name = "Search", description = "검색 및 브라우징 관련 API")
public class SearchController {

    private static final int MAX_LATEST_PAGE_SIZE = 60;

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * 전문 검색을 수행합니다.
     *
     * Requirements: 8.1 - 책 제목, 작가, 출판사, 디자이너 이름, ISBN으로 검색 결과를 반환한다
     */
    @GetMapping
    @Operation(summary = "검색", description = "책 제목, 작가, 출판사, 디자이너 이름, ISBN으로 검색합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검색 성공")
    })
    public ResponseEntity<SearchResult> search(
            @Parameter(description = "검색어")
            @RequestParam(required = false) String query,

            @Parameter(description = "국가 코드 (ISO 3166-1)")
            @RequestParam(required = false) String countryCode,

            @Parameter(description = "언어 코드 (ISO 639-1)")
            @RequestParam(required = false) String languageCode,

            @Parameter(description = "시작 연도")
            @RequestParam(required = false) Integer yearFrom,

            @Parameter(description = "종료 연도")
            @RequestParam(required = false) Integer yearTo,

            @Parameter(description = "장르")
            @RequestParam(required = false) String genre,

            @Parameter(description = "출판사 ID")
            @RequestParam(required = false) UUID publisherId,

            @Parameter(description = "태그 ID 목록")
            @RequestParam(required = false) List<UUID> tagIds,

            @Parameter(description = "정렬 옵션")
            @RequestParam(required = false, defaultValue = "RELEVANCE") SortOption sortBy,

            @PageableDefault(size = 20) Pageable pageable) {

        SearchFilters filters = new SearchFilters(
                countryCode,
                languageCode,
                yearFrom,
                yearTo,
                genre,
                publisherId,
                tagIds,
                sortBy
        );

        SearchResult result = searchService.search(query, filters, pageable);
        return ResponseEntity.ok(result);
    }

    /**
     * 첫 화면에서 공개된 원서·번역서 대표 표지와 리디자인을 최신순으로 보여줍니다.
     *
     * Requirements: 8.3 - 등록일순 정렬
     */
    @GetMapping("/browse/latest")
    @Operation(summary = "최신 북커버 조회", description = "공개된 원서·번역서 대표 표지와 리디자인을 등록일 내림차순으로 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 페이지 번호 또는 크기")
    })
    public ResponseEntity<BrowseResult> browseLatest(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        if (page < 0 || size < 1 || size > MAX_LATEST_PAGE_SIZE) {
            throw new ValidationException("페이지 번호 또는 크기가 올바르지 않습니다",
                    Map.of("page", page, "size", size, "maxSize", MAX_LATEST_PAGE_SIZE));
        }
        Page<BookCoverDocument> documents = searchService.browseLatest(PageRequest.of(page, size));
        return ResponseEntity.ok(toBrowseResult("latest", "all", documents, documents.getTotalElements()));
    }

    /**
     * 국가별 브라우징을 수행합니다.
     *
     * Requirements: 8.5 - 국가별 브라우징 페이지를 제공하여 특정 국가의 모든 북커버를 조회할 수 있게 한다
     */
    @GetMapping("/browse/country/{countryCode}")
    @Operation(summary = "국가별 브라우징", description = "특정 국가의 모든 북커버를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<BrowseResult> browseByCountry(
            @Parameter(description = "국가 코드 (ISO 3166-1)")
            @PathVariable String countryCode,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<BookCoverDocument> documents = searchService.browseByCountry(countryCode, pageable);
        long totalCount = searchService.countByCountry(countryCode);

        BrowseResult result = toBrowseResult("country", countryCode, documents, totalCount);
        return ResponseEntity.ok(result);
    }

    /**
     * 연도별 브라우징을 수행합니다.
     *
     * Requirements: 8.6 - 연도별 브라우징 페이지를 제공하여 특정 연도의 모든 북커버를 조회할 수 있게 한다
     */
    @GetMapping("/browse/year/{year}")
    @Operation(summary = "연도별 브라우징", description = "특정 연도의 모든 북커버를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<BrowseResult> browseByYear(
            @Parameter(description = "출판 연도")
            @PathVariable int year,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<BookCoverDocument> documents = searchService.browseByYear(year, pageable);
        long totalCount = searchService.countByYear(year);

        BrowseResult result = toBrowseResult("year", String.valueOf(year), documents, totalCount);
        return ResponseEntity.ok(result);
    }

    /**
     * 장르별 브라우징을 수행합니다.
     *
     * Requirements: 8.7 - 장르별 브라우징 페이지를 제공하여 특정 장르의 모든 북커버를 조회할 수 있게 한다
     */
    @GetMapping("/browse/genre/{genre}")
    @Operation(summary = "장르별 브라우징", description = "특정 장르의 모든 북커버를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<BrowseResult> browseByGenre(
            @Parameter(description = "장르")
            @PathVariable String genre,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<BookCoverDocument> documents = searchService.browseByGenre(genre, pageable);
        long totalCount = searchService.countByGenre(genre);

        BrowseResult result = toBrowseResult("genre", genre, documents, totalCount);
        return ResponseEntity.ok(result);
    }

    /**
     * 태그별 브라우징을 수행합니다.
     */
    @GetMapping("/browse/tag/{tagId}")
    @Operation(summary = "태그별 브라우징", description = "특정 태그의 모든 북커버를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<BrowseResult> browseByTag(
            @Parameter(description = "태그 ID")
            @PathVariable UUID tagId,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<BookCoverDocument> documents = searchService.browseByTag(tagId, pageable);

        BrowseResult result = toBrowseResult("tag", tagId.toString(), documents, documents.getTotalElements());
        return ResponseEntity.ok(result);
    }

    /**
     * BookCoverDocument 페이지를 BrowseResult로 변환합니다.
     */
    private BrowseResult toBrowseResult(String category, String categoryValue,
                                        Page<BookCoverDocument> documents, long totalCount) {
        List<BrowseResultItem> items = documents.getContent().stream()
                .map(this::toBrowseResultItem)
                .toList();

        return new BrowseResult(
                category,
                categoryValue,
                totalCount,
                items,
                documents.getNumber(),
                documents.getSize(),
                documents.getTotalPages()
        );
    }

    /**
     * BookCoverDocument를 BrowseResultItem으로 변환합니다.
     */
    private BrowseResultItem toBrowseResultItem(BookCoverDocument doc) {
        return new BrowseResultItem(
                doc.getId(),
                doc.getType() != null ?
                        com.bookcover.archive.search.document.DocumentType.valueOf(doc.getType()) : null,
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
}
