package com.bookcover.archive.exception;

/** 검색 저장소 장애를 호출자에게 전달합니다. */
public class SearchException extends RuntimeException {
    public SearchException(Throwable cause) {
        super("검색 서비스를 사용할 수 없습니다", cause);
    }
}
