package com.bookcover.archive.controller.dto.contributor;

import com.bookcover.archive.domain.book.BookCover;
import com.bookcover.archive.domain.book.CoverType;
import com.bookcover.archive.domain.book.CreditRole;
import com.bookcover.archive.domain.book.OriginalBook;
import com.bookcover.archive.domain.media.Media;

import java.util.List;
import java.util.UUID;

/**
 * 디자이너가 크레딧에 오른 표지 한 장. 같은 표지에서 맡은 역할은 roles로 묶는다.
 *
 * Requirements: 10.3 - 디자이너가 디자인한 모든 북커버 목록 표시
 */
public record DesignerCoverResponse(
    UUID coverId,
    String thumbnailUrl,
    String imageUrl,
    CoverType coverType,
    String label,
    List<CreditRole> roles,
    BookSummary book
) {
    /** type은 ORIGINAL 또는 TRANSLATED. 상세는 /books/original/{id}, /books/translated/{id} */
    public record BookSummary(UUID id, String type, String title, String countryCode, Integer year) {}

    public static DesignerCoverResponse from(BookCover cover, UUID designerId, Media book) {
        return new DesignerCoverResponse(
            cover.getId(),
            cover.getThumbnailUrl(),
            cover.getImageUrl(),
            cover.getCoverType(),
            cover.getLabel(),
            cover.getCredits().stream()
                .filter(c -> c.getDesigner().getId().equals(designerId))
                .map(c -> c.getRole())
                .sorted()
                .toList(),
            book == null ? null : new BookSummary(
                book.getId(),
                book instanceof OriginalBook ? "ORIGINAL" : "TRANSLATED",
                book.getTitle(),
                book.getCountryCode(),
                book.getYear())
        );
    }
}
