package com.bookcover.archive.exception;

import java.util.UUID;

/**
 * ISBN 중복 시 발생하는 예외.
 *
 * Requirements: 12.1 - 동일한 ISBN이 이미 존재하면 중복 경고를 표시하고 기존 엔트리 정보를 보여준다
 */
public class DuplicateIsbnException extends RuntimeException {

    private final UUID existingBookId;
    private final String isbn;

    public DuplicateIsbnException(String isbn, UUID existingBookId) {
        super("ISBN already exists: " + isbn);
        this.isbn = isbn;
        this.existingBookId = existingBookId;
    }

    public UUID getExistingBookId() {
        return existingBookId;
    }

    public String getIsbn() {
        return isbn;
    }
}
