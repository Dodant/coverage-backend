package com.bookcover.archive.service.impl;

import com.bookcover.archive.config.security.SecurityUtils;
import com.bookcover.archive.controller.dto.book.BookCoverResponse;
import com.bookcover.archive.controller.dto.book.OriginalBookResponse;
import com.bookcover.archive.controller.dto.book.TranslatedBookResponse;
import com.bookcover.archive.domain.audit.AuditEntityType;
import com.bookcover.archive.domain.book.*;
import com.bookcover.archive.domain.common.ContentStatus;
import com.bookcover.archive.domain.media.Media;
import com.bookcover.archive.event.ContentEvent;
import com.bookcover.archive.event.ContentEventType;
import com.bookcover.archive.event.EventPublisher;
import com.bookcover.archive.exception.ResourceNotFoundException;
import com.bookcover.archive.exception.ValidationException;
import com.bookcover.archive.repository.BookCoverRepository;
import com.bookcover.archive.service.AuditService;
import com.bookcover.archive.service.CatalogService;
import com.bookcover.archive.service.dto.UpdateCoverMetadataRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

/** 관리자 도서 목록의 필터는 DB에서 페이지 처리 전에 적용한다. */
@Service
@Transactional
public class CatalogServiceImpl implements CatalogService {
    private final EntityManager em;
    private final BookCoverRepository covers;
    private final AuditService audit;
    private final ObjectMapper mapper;
    private final EventPublisher events;

    public CatalogServiceImpl(EntityManager em, BookCoverRepository covers, AuditService audit, ObjectMapper mapper, EventPublisher events) {
        this.em = em;
        this.covers = covers;
        this.audit = audit;
        this.mapper = mapper;
        this.events = events;
    }

    // 원서와 번역서의 공통 목록. 사용자 입력은 전부 바인딩하며 정렬과 보완 조건은 고정 목록만 쓴다.
    private static final String FROM = """
        FROM media m
        LEFT JOIN original_books o ON o.id = m.id
        LEFT JOIN translated_books t ON t.id = m.id
        LEFT JOIN authors a ON a.id = COALESCE(o.author_id, t.author_id)
        LEFT JOIN publishers p ON p.id = COALESCE(o.publisher_id, t.publisher_id)
        LEFT JOIN book_covers c ON c.id = COALESCE(o.front_cover_id, t.front_cover_id)
        WHERE (o.id IS NOT NULL OR t.id IS NOT NULL)
        """;

    /** Java 표시 판정과 SQL 필터가 같은 기준을 쓴다. 해상도와 크레딧은 대표 표지 기준이다. */
    public static List<Issue> issues(Media book) {
        List<Issue> result = new ArrayList<>();
        BookCover c = book.getFrontCover();
        if (c == null || blank(c.getImageUrl())) result.add(Issue.MISSING_COVER);
        else if (c.isHidden()) result.add(Issue.HIDDEN_COVER);
        else if (c.getWidth() == null || c.getHeight() == null || c.getWidth() <= 0 || c.getHeight() <= 0)
            result.add(Issue.UNKNOWN_RESOLUTION);
        else if (c.getWidth() < 800 || c.getHeight() < 1200) result.add(Issue.LOW_RESOLUTION);
        if (blank(isbn(book))) result.add(Issue.MISSING_ISBN);
        if (c != null && blank(c.getSourceUrl())) result.add(Issue.MISSING_SOURCE);
        if (c != null && c.getCredits().isEmpty()) result.add(Issue.MISSING_CREDIT);
        if (book.getYear() == null) result.add(Issue.MISSING_YEAR);
        if (publisher(book) == null) result.add(Issue.MISSING_PUBLISHER);
        if (book.getCovers().size() > 1 && book.getCovers().stream().anyMatch(v -> v.getVariantType() == CoverVariantType.UNKNOWN))
            result.add(Issue.UNCLASSIFIED_VARIANT);
        return result;
    }

    private static String issueSql(Issue issue) {
        String visible = "c.id IS NOT NULL AND TRIM(COALESCE(c.image_url, '')) <> '' AND NOT c.hidden";
        return switch (issue) {
            case MISSING_COVER -> "(c.id IS NULL OR TRIM(COALESCE(c.image_url, '')) = '')";
            case HIDDEN_COVER -> "(TRIM(COALESCE(c.image_url, '')) <> '' AND c.hidden)";
            case LOW_RESOLUTION -> "(" + visible + " AND c.width > 0 AND c.height > 0 AND (c.width < 800 OR c.height < 1200))";
            case UNKNOWN_RESOLUTION -> "(" + visible + " AND (c.width IS NULL OR c.height IS NULL OR c.width <= 0 OR c.height <= 0))";
            case MISSING_ISBN -> "TRIM(COALESCE(o.isbn, t.isbn, '')) = ''";
            case MISSING_SOURCE -> "(c.id IS NOT NULL AND TRIM(COALESCE(c.source_url, '')) = '')";
            case MISSING_CREDIT -> "(c.id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM cover_credits cr WHERE cr.cover_id = c.id))";
            case MISSING_YEAR -> "m.year IS NULL";
            case MISSING_PUBLISHER -> "p.id IS NULL";
            case UNCLASSIFIED_VARIANT -> "((SELECT COUNT(*) FROM book_covers v WHERE v.book_id = m.id) > 1 AND EXISTS (SELECT 1 FROM book_covers v WHERE v.book_id = m.id AND (v.variant_type IS NULL OR v.variant_type = 'UNKNOWN')))";
        };
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Item> list(Filter f, int page, int size, String sort) {
        requireAdmin();
        Map<String, Object> params = new LinkedHashMap<>();
        StringBuilder where = new StringBuilder(FROM);
        if (!blank(f.query())) {
            where.append(" AND (LOWER(m.title) LIKE :query ESCAPE '!' OR LOWER(COALESCE(a.name, '')) LIKE :query ESCAPE '!' OR REPLACE(REPLACE(COALESCE(o.isbn, t.isbn, ''), '-', ''), ' ', '') LIKE :isbn ESCAPE '!')");
            params.put("query", like(f.query()));
            params.put("isbn", like(f.query().replaceAll("[-\\s]", "")));
        }
        if (f.kind() != null) where.append(f.kind() == Kind.ORIGINAL ? " AND o.id IS NOT NULL" : " AND t.id IS NOT NULL");
        if (f.status() != null) { where.append(" AND m.status = :status"); params.put("status", f.status().name()); }
        if (!blank(f.language())) { where.append(" AND LOWER(m.language_code) = :language"); params.put("language", f.language().trim().toLowerCase(Locale.ROOT)); }
        if (!blank(f.publisher())) { where.append(" AND LOWER(p.name) LIKE :publisher ESCAPE '!'"); params.put("publisher", like(f.publisher())); }
        if (f.hasCover() != null) {
            String usable = "c.id IS NOT NULL AND TRIM(COALESCE(c.image_url, '')) <> '' AND NOT c.hidden";
            where.append(" AND ").append(f.hasCover() ? "(" + usable + ")" : "NOT (" + usable + ")");
        }
        if (f.issue() != null) where.append(" AND (").append(issueSql(f.issue())).append(")");
        else if (f.needsAttention()) where.append(" AND (").append(String.join(" OR ", Arrays.stream(Issue.values()).map(CatalogServiceImpl::issueSql).toList())).append(")");
        String order = switch (sort) {
            case "title" -> "m.title ASC, m.id ASC";
            case "updated" -> "m.updated_at DESC, m.id ASC";
            default -> "m.created_at DESC, m.id ASC";
        };
        int actualSize = Math.max(1, Math.min(size, 100));
        int actualPage = Math.max(0, page);
        Query ids = em.createNativeQuery("SELECT m.id " + where + " ORDER BY " + order, UUID.class);
        Query count = em.createNativeQuery("SELECT COUNT(*) " + where);
        params.forEach((key, value) -> { ids.setParameter(key, value); count.setParameter(key, value); });
        long total = ((Number) count.getSingleResult()).longValue();
        List<?> rows = ids.setFirstResult(Math.multiplyExact(actualPage, actualSize)).setMaxResults(actualSize).getResultList();
        List<Item> items = rows.stream().map(id -> item(find(UUID.fromString(id.toString())))).toList();
        return new PageImpl<>(items, PageRequest.of(actualPage, actualSize), total);
    }

    @Override
    @Transactional(readOnly = true)
    public Detail detail(UUID id) {
        requireAdmin();
        Media book = find(id);
        OriginalBook original = book instanceof OriginalBook o ? o : ((TranslatedBook) book).getOriginalBook();
        List<Item> family = new ArrayList<>();
        if (original != null) {
            family.add(item(original));
            original.getTranslations().stream().sorted(Comparator.comparing(Media::getLanguageCode).thenComparing(Media::getTitle))
                    .forEach(t -> family.add(item(t)));
            if (original.getWork() != null) {
                em.createQuery("SELECT b FROM OriginalBook b WHERE b.work.id = :work AND b.id <> :id ORDER BY b.year, b.title", OriginalBook.class)
                        .setParameter("work", original.getWork().getId()).setParameter("id", original.getId())
                        .getResultList().forEach(b -> family.add(item(b)));
            }
        }
        return new Detail(kind(book), book instanceof OriginalBook o ? OriginalBookResponse.from(o) : TranslatedBookResponse.from((TranslatedBook) book), family, issues(book));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object> editions(UUID originalId) {
        requireAdmin();
        Media book = find(originalId);
        if (!(book instanceof OriginalBook original)) throw new ValidationException("원서 ID가 필요합니다", Map.of());
        List<Object> books = new ArrayList<>();
        books.add(OriginalBookResponse.from(original));
        original.getTranslations().forEach(t -> books.add(TranslatedBookResponse.from(t)));
        return books;
    }

    @Override
    public BookCoverResponse updateCover(UUID bookId, UUID coverId, UpdateCoverMetadataRequest r, UUID userId) {
        requireAdmin();
        Media book = find(bookId);
        if (book.getStatus() == ContentStatus.DELETED) throw new ValidationException("삭제된 도서의 표지는 수정할 수 없습니다", Map.of());
        BookCover c = covers.findByIdForUpdate(coverId).orElseThrow(() -> new ResourceNotFoundException("BookCover", coverId));
        boolean belongs = c.getBook() != null ? bookId.equals(c.getBook().getId())
                : book.getFrontCover() != null && coverId.equals(book.getFrontCover().getId());
        if (!belongs) throw new ValidationException("이 도서에 속한 표지가 아닙니다", Map.of("coverId", coverId));
        String before = json(c);
        c.setLabel(clean(r.label()));
        c.setPrintingFrom(r.printingFrom());
        c.setUsedFrom(r.usedFrom());
        c.setVariantType(r.variantType());
        c.setRibbonStatus(r.ribbonStatus());
        c.setSourceName(clean(r.sourceName()));
        c.setSourceUrl(clean(r.sourceUrl()));
        if (r.credits() != null) {
            Set<String> keys = new HashSet<>();
            List<CoverCredit> wanted = new ArrayList<>();
            for (var credit : r.credits()) {
                String key = credit.designerId() + ":" + credit.role();
                if (!keys.add(key)) continue;
                var designer = em.find(com.bookcover.archive.domain.contributor.Designer.class, credit.designerId());
                if (designer == null) throw new ResourceNotFoundException("Designer", credit.designerId());
                wanted.add(new CoverCredit(c, designer, credit.role()));
            }
            c.getCredits().removeIf(credit -> !keys.contains(credit.getDesigner().getId() + ":" + credit.getRole()));
            for (CoverCredit credit : wanted) {
                if (c.getCredits().stream().noneMatch(old -> old.getDesigner().getId().equals(credit.getDesigner().getId()) && old.getRole() == credit.getRole()))
                    c.getCredits().add(credit);
            }
        }
        book.setUpdatedAt(Instant.now());
        audit.logUpdate(book instanceof OriginalBook ? AuditEntityType.ORIGINAL_BOOK : AuditEntityType.TRANSLATED_BOOK,
                bookId, userId, before, json(c));
        // 크레딧 변경을 디자이너 검색 등에도 반영한다. 기존 outbox 발행 경로를 따른다.
        events.publishContentEvent(new ContentEvent(book instanceof OriginalBook ? ContentEventType.ORIGINAL_BOOK_UPDATED : ContentEventType.TRANSLATED_BOOK_UPDATED,
                bookId, book.getCreatedBy() == null ? null : book.getCreatedBy().getId()));
        return BookCoverResponse.from(c);
    }

    private String json(BookCover c) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("coverId", c.getId()); values.put("label", c.getLabel());
        values.put("printingFrom", c.getPrintingFrom()); values.put("usedFrom", c.getUsedFrom());
        values.put("variantType", c.getVariantType()); values.put("ribbonStatus", c.getRibbonStatus());
        values.put("sourceName", c.getSourceName()); values.put("sourceUrl", c.getSourceUrl());
        values.put("credits", c.getCredits().stream().map(credit -> Map.of("designerId", credit.getDesigner().getId(), "name", credit.getDesigner().getName(), "role", credit.getRole())).toList());
        try { return mapper.writeValueAsString(values); }
        catch (JsonProcessingException e) { throw new IllegalStateException("표지 변경 이력을 만들 수 없습니다", e); }
    }
    private Media find(UUID id) {
        Media book = (Media) org.hibernate.Hibernate.unproxy(em.find(Media.class, id));
        if (!(book instanceof OriginalBook) && !(book instanceof TranslatedBook)) throw new ResourceNotFoundException("Book", id);
        return book;
    }
    static Item item(Media book) {
        return new Item(book.getId(), kind(book), book.getTitle(),
                book instanceof OriginalBook o ? o.getAuthor() == null ? null : o.getAuthor().getName() : ((TranslatedBook) book).getAuthor() == null ? null : ((TranslatedBook) book).getAuthor().getName(),
                publisher(book), book.getLanguageCode(), isbn(book), book instanceof OriginalBook o ? o.getEdition() : ((TranslatedBook) book).getEdition(),
                book.getYear(), book.getStatus(), BookCoverResponse.from(book.getFrontCover()), issues(book));
    }
    private static Kind kind(Media b) { return b instanceof OriginalBook ? Kind.ORIGINAL : Kind.TRANSLATED; }
    private static String isbn(Media b) { return b instanceof OriginalBook o ? o.getIsbn() : ((TranslatedBook) b).getIsbn(); }
    private static String publisher(Media b) { return b instanceof OriginalBook o ? o.getPublisher() == null ? null : o.getPublisher().getName() : ((TranslatedBook) b).getPublisher() == null ? null : ((TranslatedBook) b).getPublisher().getName(); }
    private static boolean blank(String s) { return s == null || s.isBlank(); }
    private static String clean(String s) { return blank(s) ? null : s.trim(); }
    private static String like(String s) { return "%" + s.trim().toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%"; }
    private static void requireAdmin() {
        if (!SecurityUtils.hasRole("ADMIN")) throw new ValidationException("관리자만 도서를 관리할 수 있습니다", Map.of());
    }
}
