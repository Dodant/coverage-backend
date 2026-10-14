package com.bookcover.archive.controller.dto.book;

/**
 * 국내 서점 구매 링크. 저장하지 않고 ISBN(정규화된 값)으로 응답 시 생성한다.
 */
public record PurchaseLinks(String aladin, String yes24, String kyobo) {

    // ponytail: 국내 3사 고정. 해외 서점이 필요하면 countryCode → 서점 목록 매핑으로 확장.
    public static PurchaseLinks fromIsbn(String isbn) {
        if (isbn == null || isbn.isBlank()) {
            return null;
        }
        return new PurchaseLinks(
            "https://www.aladin.co.kr/shop/wproduct.aspx?ISBN=" + isbn,
            "https://www.yes24.com/Product/Search?domain=BOOK&query=" + isbn,
            "https://search.kyobobook.co.kr/search?keyword=" + isbn
        );
    }
}
