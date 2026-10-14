package com.bookcover.archive.controller;

import com.bookcover.archive.domain.book.OriginalBook;
import com.bookcover.archive.domain.book.Series;
import com.bookcover.archive.domain.media.Media;
import com.bookcover.archive.exception.ResourceNotFoundException;
import com.bookcover.archive.repository.SeriesRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 시리즈·총서 조회 API. 시리즈는 원서·번역서 등록/수정 시 seriesId 또는 seriesName으로 만들어진다.
 */
@RestController
@RequestMapping("/series")
@Tag(name = "Series", description = "시리즈·총서 관련 API")
public class SeriesController {

    private final SeriesRepository seriesRepository;

    public SeriesController(SeriesRepository seriesRepository) {
        this.seriesRepository = seriesRepository;
    }

    @GetMapping
    @Operation(summary = "시리즈 목록 조회", description = "이름으로 필터링할 수 있습니다.")
    public ResponseEntity<Page<SeriesResponse>> getSeriesList(
            @RequestParam(required = false) String name,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Series> page = (name != null && !name.isBlank())
                ? seriesRepository.findByNameContainingIgnoreCase(name, pageable)
                : seriesRepository.findAll(pageable);
        return ResponseEntity.ok(page.map(SeriesResponse::from));
    }

    @GetMapping("/{id}")
    @Operation(summary = "시리즈 조회")
    public ResponseEntity<SeriesResponse> getSeries(@PathVariable UUID id) {
        return ResponseEntity.ok(SeriesResponse.from(findSeries(id)));
    }

    @GetMapping("/{id}/books")
    @Operation(summary = "시리즈 책 목록", description = "시리즈에 속한 원서·번역서를 권 번호순으로 조회합니다.")
    public ResponseEntity<List<SeriesBookItem>> getSeriesBooks(@PathVariable UUID id) {
        findSeries(id);
        // ponytail: 페이징 없음. 시리즈 하나가 수백 권을 넘기 시작하면 Pageable로 바꾼다
        List<SeriesBookItem> books = seriesRepository.findBooks(id).stream()
                .map(SeriesBookItem::from)
                .sorted(SERIES_ORDER)
                .toList();
        return ResponseEntity.ok(books);
    }

    /** 권 번호의 앞자리 숫자순("3" < "12"), 숫자가 없거나 같으면 문자열·연도순. 번호 없는 책은 뒤로 */
    static final Comparator<SeriesBookItem> SERIES_ORDER = Comparator
            .comparing((SeriesBookItem b) -> leadingNumber(b.seriesNumber()), Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(SeriesBookItem::seriesNumber, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(SeriesBookItem::year, Comparator.nullsLast(Comparator.naturalOrder()));

    static Long leadingNumber(String seriesNumber) {
        if (seriesNumber == null) {
            return null;
        }
        Matcher m = LEADING_DIGITS.matcher(seriesNumber.trim());
        return m.lookingAt() ? Long.parseLong(m.group()) : null;
    }

    private static final Pattern LEADING_DIGITS = Pattern.compile("\\d{1,18}");

    private Series findSeries(UUID id) {
        return seriesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Series", id));
    }

    public record SeriesResponse(UUID id, String name, UUID publisherId, String publisherName) {
        static SeriesResponse from(Series series) {
            return new SeriesResponse(
                    series.getId(),
                    series.getName(),
                    series.getPublisher() != null ? series.getPublisher().getId() : null,
                    series.getPublisher() != null ? series.getPublisher().getName() : null);
        }
    }

    /** type은 ORIGINAL 또는 TRANSLATED. 상세는 /books/original/{id}, /books/translated/{id} */
    public record SeriesBookItem(UUID id, String type, String title, String seriesNumber,
                                 Integer year, String countryCode, String thumbnailUrl) {
        static SeriesBookItem from(Media book) {
            return new SeriesBookItem(
                    book.getId(),
                    Hibernate.unproxy(book) instanceof OriginalBook ? "ORIGINAL" : "TRANSLATED",
                    book.getTitle(),
                    book.getSeriesNumber(),
                    book.getYear(),
                    book.getCountryCode(),
                    book.getFrontCover() != null ? book.getFrontCover().getThumbnailUrl() : null);
        }
    }
}
