package com.bookcover.archive.exception;

/**
 * 레이트 리밋 초과 시 발생하는 예외.
 *
 * Requirements:
 * - 18.5: 레이트 리밋에 도달하면 사용자에게 대기 시간을 안내한다
 */
public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;
    private final int limit;

    public RateLimitExceededException(String message, long retryAfterSeconds, int limit) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
        this.limit = limit;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public int getLimit() {
        return limit;
    }
}
