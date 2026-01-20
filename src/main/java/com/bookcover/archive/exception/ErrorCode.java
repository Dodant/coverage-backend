package com.bookcover.archive.exception;

import org.springframework.http.HttpStatus;

/**
 * 에러 코드 상수 정의.
 *
 * Requirements: 8.4 - 검색 결과가 없으면 적절한 안내 메시지를 표시한다
 */
public enum ErrorCode {

    // 400 Bad Request
    VALIDATION_ERROR("VALIDATION_ERROR", "입력 검증 실패", HttpStatus.BAD_REQUEST),
    INVALID_ISBN("INVALID_ISBN", "잘못된 ISBN 형식", HttpStatus.BAD_REQUEST),
    INVALID_IMAGE("INVALID_IMAGE", "이미지 검증 실패", HttpStatus.BAD_REQUEST),
    INVALID_IMAGE_RESOLUTION("INVALID_IMAGE_RESOLUTION", "이미지 해상도가 최소 기준 미만입니다", HttpStatus.BAD_REQUEST),
    INVALID_IMAGE_FORMAT("INVALID_IMAGE_FORMAT", "지원되지 않는 이미지 형식입니다", HttpStatus.BAD_REQUEST),
    INVALID_IMAGE_SIZE("INVALID_IMAGE_SIZE", "파일 크기가 최대 제한을 초과합니다", HttpStatus.BAD_REQUEST),
    EMPTY_COMMENT("EMPTY_COMMENT", "댓글 내용이 비어있습니다", HttpStatus.BAD_REQUEST),
    MISSING_REQUIRED_FIELD("MISSING_REQUIRED_FIELD", "필수 항목이 누락되었습니다", HttpStatus.BAD_REQUEST),
    MISSING_FRONT_COVER("MISSING_FRONT_COVER", "앞면 북커버 이미지가 필요합니다", HttpStatus.BAD_REQUEST),

    // 401 Unauthorized
    UNAUTHORIZED("UNAUTHORIZED", "인증이 필요합니다", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("INVALID_TOKEN", "유효하지 않은 토큰입니다", HttpStatus.UNAUTHORIZED),
    EXPIRED_TOKEN("EXPIRED_TOKEN", "만료된 토큰입니다", HttpStatus.UNAUTHORIZED),

    // 403 Forbidden
    FORBIDDEN("FORBIDDEN", "권한이 없습니다", HttpStatus.FORBIDDEN),
    CONTENT_HIDDEN("CONTENT_HIDDEN", "비공개 콘텐츠입니다", HttpStatus.FORBIDDEN),
    USER_SUSPENDED("USER_SUSPENDED", "정지된 계정입니다", HttpStatus.FORBIDDEN),
    REPORT_RESTRICTED("REPORT_RESTRICTED", "신고 기능이 제한되었습니다", HttpStatus.FORBIDDEN),

    // 404 Not Found
    NOT_FOUND("NOT_FOUND", "리소스를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    BOOK_NOT_FOUND("BOOK_NOT_FOUND", "책을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    USER_NOT_FOUND("USER_NOT_FOUND", "사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    COLLECTION_NOT_FOUND("COLLECTION_NOT_FOUND", "컬렉션을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    COMMENT_NOT_FOUND("COMMENT_NOT_FOUND", "댓글을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    NO_SEARCH_RESULTS("NO_SEARCH_RESULTS", "검색 결과가 없습니다", HttpStatus.NOT_FOUND),

    // 409 Conflict
    DUPLICATE_ISBN("DUPLICATE_ISBN", "ISBN이 이미 존재합니다", HttpStatus.CONFLICT),
    DUPLICATE_EMAIL("DUPLICATE_EMAIL", "이메일이 이미 존재합니다", HttpStatus.CONFLICT),
    DUPLICATE_ENTRY("DUPLICATE_ENTRY", "중복된 항목입니다", HttpStatus.CONFLICT),

    // 429 Too Many Requests
    RATE_LIMITED("RATE_LIMITED", "요청 제한을 초과했습니다", HttpStatus.TOO_MANY_REQUESTS),

    // 500 Internal Server Error
    INTERNAL_ERROR("INTERNAL_ERROR", "서버 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    IMAGE_PROCESSING_ERROR("IMAGE_PROCESSING_ERROR", "이미지 처리 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    SEARCH_ERROR("SEARCH_ERROR", "검색 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String defaultMessage, HttpStatus httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
