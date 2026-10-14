package com.bookcover.archive.controller.dto.book;

import com.bookcover.archive.domain.book.OriginalBook;
import com.bookcover.archive.domain.common.ContentStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 원서 응답 DTO.
 */
public record OriginalBookResponse(
    UUID id,
    String title,
    String originalTitle,
    String countryCode,
    String languageCode,
    Integer year,
    String genre,
    String isbn,
    PurchaseLinks purchaseLinks,
    String edition,
    ContributorInfo author,
    ContributorInfo publisher,
    ContributorInfo designer,
    UUID workId,
    BookCoverResponse frontCover,
    BookCoverResponse backCover,
    List<BookCoverResponse> covers,
    ContentStatus status,
    int translationCount,
    Instant createdAt,
    Instant updatedAt
) {
    public static OriginalBookResponse from(OriginalBook book) {
        return new OriginalBookResponse(
            book.getId(),
            book.getTitle(),
            book.getOriginalTitle(),
            book.getCountryCode(),
            book.getLanguageCode(),
            book.getYear(),
            book.getGenre(),
            book.getIsbn(),
            PurchaseLinks.fromIsbn(book.getIsbn()),
            book.getEdition(),
            book.getAuthor() != null ? new ContributorInfo(book.getAuthor().getId(), book.getAuthor().getName()) : null,
            book.getPublisher() != null ? new ContributorInfo(book.getPublisher().getId(), book.getPublisher().getName()) : null,
            book.getDesigner() != null ? new ContributorInfo(book.getDesigner().getId(), book.getDesigner().getName()) : null,
            book.getWork() != null ? book.getWork().getId() : null,
            BookCoverResponse.from(book.getFrontCover()),
            book.getBackCover() != null ? BookCoverResponse.from(book.getBackCover()) : null,
            book.getCovers().stream().map(BookCoverResponse::from).toList(),
            book.getStatus(),
            book.getTranslations() != null ? book.getTranslations().size() : 0,
            book.getCreatedAt(),
            book.getUpdatedAt()
        );
    }

    public record ContributorInfo(UUID id, String name) {}
}
