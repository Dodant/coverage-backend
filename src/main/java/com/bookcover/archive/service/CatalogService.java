package com.bookcover.archive.service;

import com.bookcover.archive.controller.dto.book.BookCoverResponse;
import com.bookcover.archive.domain.common.ContentStatus;
import com.bookcover.archive.service.dto.UpdateCoverMetadataRequest;
import org.springframework.data.domain.Page;
import java.util.List;
import java.util.UUID;

/** 백오피스 전체 도서와 보완 작업 목록. 공개 검색 인덱스에 의존하지 않는다. */
public interface CatalogService {
    enum Kind { ORIGINAL, TRANSLATED }
    enum Issue {
        MISSING_COVER, HIDDEN_COVER, LOW_RESOLUTION, UNKNOWN_RESOLUTION,
        MISSING_ISBN, MISSING_SOURCE, MISSING_CREDIT, MISSING_YEAR, MISSING_PUBLISHER,
        UNCLASSIFIED_VARIANT
    }
    record Filter(String query, Kind kind, ContentStatus status, String language, String publisher,
                  Boolean hasCover, Issue issue, boolean needsAttention) {}
    record Item(UUID id, Kind kind, String title, String author, String publisher, String languageCode,
                String isbn, String edition, Integer year, ContentStatus status, BookCoverResponse frontCover,
                List<Issue> issues) {}
    record Detail(Kind kind, Object book, List<Item> relatedBooks, List<Issue> issues) {}
    Page<Item> list(Filter filter, int page, int size, String sort);
    Detail detail(UUID id);
    List<Object> editions(UUID originalId);
    BookCoverResponse updateCover(UUID bookId, UUID coverId, UpdateCoverMetadataRequest request, UUID userId);
}
