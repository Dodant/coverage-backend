package com.bookcover.archive.service.impl;

import com.bookcover.archive.config.RateLimitConfig;
import com.bookcover.archive.exception.RateLimitExceededException;
import com.bookcover.archive.service.RateLimitAction;
import com.bookcover.archive.service.RateLimitResult;
import com.bookcover.archive.service.RateLimitService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Redis 기반 레이트 리밋 서비스 구현체.
 * 슬라이딩 윈도우 방식으로 사용자별 액션 제한을 관리합니다.
 *
 * Requirements:
 * - 18.1: 댓글, 좋아요, 신고, 업로드에 대해 사용자 단위 레이트 리밋을 적용한다
 * - 18.5: 레이트 리밋에 도달하면 사용자에게 대기 시간을 안내한다
 */
@Service
public class RateLimitServiceImpl implements RateLimitService {

    private static final String KEY_PREFIX = "rate_limit:";
    private static final String RESTRICTED_KEY_PREFIX = "restricted:";

    private final StringRedisTemplate redisTemplate;
    private final RateLimitConfig rateLimitConfig;

    public RateLimitServiceImpl(StringRedisTemplate redisTemplate, RateLimitConfig rateLimitConfig) {
        this.redisTemplate = redisTemplate;
        this.rateLimitConfig = rateLimitConfig;
    }

    @Override
    public RateLimitResult checkLimit(UUID userId, RateLimitAction action) {
        String key = buildKey(userId, action);
        RateLimitConfig.ActionLimit config = getActionConfig(action);

        Long currentCount = redisTemplate.opsForValue().increment(key, 0);
        if (currentCount == null) {
            currentCount = 0L;
        }

        Long ttl = redisTemplate.getExpire(key);
        Instant resetTime = calculateResetTime(ttl, config.getWindowSeconds());

        if (currentCount >= config.getLimit()) {
            return RateLimitResult.denied(resetTime);
        }

        int remaining = config.getLimit() - currentCount.intValue();
        return RateLimitResult.allowed(remaining, resetTime);
    }

    @Override
    public void recordAction(UUID userId, RateLimitAction action) {
        String key = buildKey(userId, action);
        RateLimitConfig.ActionLimit config = getActionConfig(action);

        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            // 첫 번째 요청인 경우 TTL 설정
            redisTemplate.expire(key, Duration.ofSeconds(config.getWindowSeconds()));
        }
    }

    @Override
    public boolean isUserRestricted(UUID userId) {
        String key = RESTRICTED_KEY_PREFIX + userId.toString();
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public RateLimitResult checkAndRecord(UUID userId, RateLimitAction action) {
        RateLimitResult result = checkLimit(userId, action);

        if (!result.allowed()) {
            RateLimitConfig.ActionLimit config = getActionConfig(action);
            long retryAfterSeconds = calculateRetryAfterSeconds(result.resetTime());
            throw new RateLimitExceededException(
                    "Rate limit exceeded for " + action.name().toLowerCase(),
                    retryAfterSeconds,
                    config.getLimit()
            );
        }

        recordAction(userId, action);
        return result;
    }

    private String buildKey(UUID userId, RateLimitAction action) {
        return KEY_PREFIX + action.name().toLowerCase() + ":" + userId.toString();
    }

    private RateLimitConfig.ActionLimit getActionConfig(RateLimitAction action) {
        return switch (action) {
            case UPLOAD -> rateLimitConfig.getUpload();
            case COMMENT -> rateLimitConfig.getComment();
            case LIKE -> rateLimitConfig.getLike();
            case REPORT -> rateLimitConfig.getReport();
        };
    }

    private Instant calculateResetTime(Long ttl, int windowSeconds) {
        if (ttl == null || ttl < 0) {
            return Instant.now().plusSeconds(windowSeconds);
        }
        return Instant.now().plusSeconds(ttl);
    }

    private long calculateRetryAfterSeconds(Instant resetTime) {
        long seconds = Duration.between(Instant.now(), resetTime).getSeconds();
        return Math.max(1, seconds);
    }
}
