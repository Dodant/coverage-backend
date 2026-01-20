package com.bookcover.archive.exception;

import java.util.HashMap;
import java.util.Map;

/**
 * 입력 검증 실패 시 발생하는 예외.
 */
public class ValidationException extends RuntimeException {

    private final Map<String, Object> errors;

    public ValidationException(String message) {
        super(message);
        this.errors = new HashMap<>();
    }

    public ValidationException(String message, Map<String, Object> errors) {
        super(message);
        this.errors = errors != null ? errors : new HashMap<>();
    }

    public Map<String, Object> getErrors() {
        return errors;
    }
}
