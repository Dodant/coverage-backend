package com.bookcover.archive.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

/**
 * 전역 예외 처리기.
 *
 * Requirements:
 * - 1.3: 앞면 북커버 이미지가 누락된 경우 등록을 거부하고 오류 메시지를 표시한다
 * - 14.2: 이미지 해상도가 최소 기준 미만이면 업로드를 거부하고 권장 해상도를 안내한다
 * - 14.6: 이미지 파일 크기가 최대 제한을 초과하면 업로드를 거부하고 파일 크기 제한을 안내한다
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * ValidationException 처리 - 입력 검증 실패
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        ErrorResponse response = new ErrorResponse(
            ErrorCode.VALIDATION_ERROR,
            ex.getMessage() != null ? ex.getMessage() : ErrorCode.VALIDATION_ERROR.getDefaultMessage(),
            ex.getErrors()
        );
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * DuplicateIsbnException 처리 - ISBN 중복
     */
    @ExceptionHandler(DuplicateIsbnException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateIsbn(DuplicateIsbnException ex) {
        Map<String, Object> details = new HashMap<>();
        details.put("existingBookId", ex.getExistingBookId());
        details.put("isbn", ex.getIsbn());

        ErrorResponse response = new ErrorResponse(
            ErrorCode.DUPLICATE_ISBN,
            ex.getMessage(),
            details
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * RateLimitExceededException 처리 - 요청 제한 초과
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitExceededException ex) {
        Map<String, Object> details = new HashMap<>();
        details.put("retryAfter", ex.getRetryAfterSeconds());
        details.put("limit", ex.getLimit());

        ErrorResponse response = new ErrorResponse(
            ErrorCode.RATE_LIMITED,
            ex.getMessage(),
            details
        );
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
            .body(response);
    }

    /**
     * ResourceNotFoundException 처리 - 리소스 없음
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        Map<String, Object> details = new HashMap<>();
        details.put("resourceType", ex.getResourceType());
        details.put("fieldName", ex.getFieldName());
        details.put("fieldValue", ex.getFieldValue());

        ErrorResponse response = new ErrorResponse(
            ErrorCode.NOT_FOUND,
            ex.getMessage(),
            details
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }


    /**
     * MethodArgumentNotValidException 처리 - Bean Validation 실패
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, Object> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = error instanceof FieldError
                ? ((FieldError) error).getField()
                : error.getObjectName();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse response = new ErrorResponse(
            ErrorCode.VALIDATION_ERROR,
            "입력 검증에 실패했습니다",
            errors
        );
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * MissingServletRequestParameterException 처리 - 필수 파라미터 누락
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex) {
        Map<String, Object> details = new HashMap<>();
        details.put("parameterName", ex.getParameterName());
        details.put("parameterType", ex.getParameterType());

        ErrorResponse response = new ErrorResponse(
            ErrorCode.MISSING_REQUIRED_FIELD,
            "필수 파라미터가 누락되었습니다: " + ex.getParameterName(),
            details
        );
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * MaxUploadSizeExceededException 처리 - 파일 크기 초과
     * Requirements: 14.6 - 이미지 파일 크기가 최대 제한을 초과하면 업로드를 거부하고 파일 크기 제한을 안내한다
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        Map<String, Object> details = new HashMap<>();
        details.put("maxSize", "10MB");
        details.put("recommendation", "파일 크기를 10MB 이하로 줄여주세요");

        ErrorResponse response = new ErrorResponse(
            ErrorCode.INVALID_IMAGE_SIZE,
            "파일 크기가 최대 제한(10MB)을 초과합니다",
            details
        );
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * AuthenticationException 처리 - 인증 실패
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        ErrorResponse response = new ErrorResponse(
            ErrorCode.UNAUTHORIZED,
            "인증에 실패했습니다"
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * BadCredentialsException 처리 - 잘못된 자격 증명
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        ErrorResponse response = new ErrorResponse(
            ErrorCode.UNAUTHORIZED,
            "이메일 또는 비밀번호가 올바르지 않습니다"
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * AccessDeniedException 처리 - 접근 거부
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        ErrorResponse response = new ErrorResponse(
            ErrorCode.FORBIDDEN,
            "접근 권한이 없습니다"
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * IllegalArgumentException 처리 - 잘못된 인자
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse response = new ErrorResponse(
            ErrorCode.VALIDATION_ERROR,
            ex.getMessage()
        );
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * IllegalStateException 처리 - 잘못된 상태
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        ErrorResponse response = new ErrorResponse(
            ErrorCode.VALIDATION_ERROR,
            ex.getMessage()
        );
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 기타 모든 예외 처리 - 서버 오류
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse response = new ErrorResponse(
            ErrorCode.INTERNAL_ERROR,
            "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
