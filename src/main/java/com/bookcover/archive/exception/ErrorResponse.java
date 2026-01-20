package com.bookcover.archive.exception;

import java.time.Instant;
import java.util.Map;

/**
 * 표준 에러 응답 형식.
 *
 * Requirements:
 * - 1.3: 앞면 북커버 이미지가 누락된 경우 오류 메시지를 표시한다
 * - 14.2: 이미지 해상도가 최소 기준 미만이면 업로드를 거부하고 권장 해상도를 안내한다
 * - 14.6: 이미지 파일 크기가 최대 제한을 초과하면 업로드를 거부하고 파일 크기 제한을 안내한다
 */
public record ErrorResponse(
    String code,
    String message,
    Map<String, Object> details,
    Instant timestamp
) {
    public ErrorResponse(String code, String message, Map<String, Object> details) {
        this(code, message, details, Instant.now());
    }

    public ErrorResponse(String code, String message) {
        this(code, message, null, Instant.now());
    }

    public ErrorResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getDefaultMessage(), null, Instant.now());
    }

    public ErrorResponse(ErrorCode errorCode, String message) {
        this(errorCode.getCode(), message, null, Instant.now());
    }

    public ErrorResponse(ErrorCode errorCode, Map<String, Object> details) {
        this(errorCode.getCode(), errorCode.getDefaultMessage(), details, Instant.now());
    }

    public ErrorResponse(ErrorCode errorCode, String message, Map<String, Object> details) {
        this(errorCode.getCode(), message, details, Instant.now());
    }
}
