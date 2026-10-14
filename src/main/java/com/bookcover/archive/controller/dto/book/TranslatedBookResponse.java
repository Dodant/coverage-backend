package com.bookcover.archive.controller.dto.book;

import com.bookcover.archive.domain.book.TranslatedBook;
import com.bookcover.archive.domain.common.ContentStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 번역서 응답 DTO.
 */
public record TranslatedBookResponse(
    UUID id,
    String title,
    String countryCode,
    String languageCode,
    Integer year,
    String isbn,
    PurchaseLinks purchaseLinks,
    String edition,
    UUID originalBookId,
    String originalBookTitle,
    ContributorInfo author,
    ContributorInfo translator,
    ContributorInfo publisher,
    ContributorInfo series,
    String seriesNumber,
    BookCoverResponse frontCover,
    BookCoverResponse backCover,
    List<BookCoverResponse> covers,
    ContentStatus status,
    Instant createdAt,
    Instant updatedAt
) {
    public static TranslatedBookResponse from(TranslatedBook book) {
        return new TranslatedBookResponse(
            book.getId(),
            book.getTitle(),
            book.getCountryCode(),
            book.getLanguageCode(),
            book.getYear(),
            book.getIsbn(),
            PurchaseLinks.fromIsbn(book.getIsbn()),
            book.getEdition(),
            book.getOriginalBook() != null ? book.getOriginalBook().getId() : null,
            book.getOriginalBook() != null ? book.getOriginalBook().getTitle() : null,
            book.getAuthor() != null ? new ContributorInfo(book.getAuthor().getId(), book.getAuthor().getName()) : null,
            book.getTranslator() != null ? new ContributorInfo(book.getTranslator().getId(), book.getTranslator().getName()) : null,
            book.getPublisher() != null ? new ContributorInfo(book.getPublisher().getId(), book.getPublisher().getName()) : null,
            book.getSeries() != null ? new ContributorInfo(book.getSeries().getId(), book.getSeries().getName()) : null,
            book.getSeriesNumber(),
            BookCoverResponse.from(book.getFrontCover()),
            book.getBackCover() != null ? BookCoverResponse.from(book.getBackCover()) : null,
            book.getCovers().stream().map(BookCoverResponse::from).toList(),
            book.getStatus(),
            book.getCreatedAt(),
            book.getUpdatedAt()
        );
    }

    public record ContributorInfo(UUID id, String name) {}
}
