package com.bookcover.archive.service.impl;

import com.bookcover.archive.config.RateLimitConfig;
import com.bookcover.archive.exception.RateLimitExceededException;
import com.bookcover.archive.service.RateLimitAction;
import com.bookcover.archive.service.RateLimitService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Redis 기반 레이트 리밋 서비스 구현체.
 * 고정 윈도우 카운터를 Lua 스크립트로 원자적으로 증가시켜 동시 요청에서도 한도를 넘지 않습니다.
 *
 * Requirements:
 * - 18.1: 댓글, 좋아요, 신고, 업로드에 대해 사용자 단위 레이트 리밋을 적용한다
 * - 18.5: 레이트 리밋에 도달하면 사용자에게 대기 시간을 안내한다
 */
@Service
public class RateLimitServiceImpl implements RateLimitService {

    private static final String KEY_PREFIX = "rate_limit:";

    // INCR와 첫 요청의 EXPIRE를 한 번에 실행. TTL이 없는 키(과거 EXPIRE 실패분)도 여기서 복구된다.
    // 반환: {현재 횟수, 남은 TTL(초)}
    private static final RedisScript<List> INCREMENT = RedisScript.of("""
            local count = redis.call('INCR', KEYS[1])
            if redis.call('TTL', KEYS[1]) < 0 then
              redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return {count, redis.call('TTL', KEYS[1])}
            """, List.class);

    // 윈도우가 이미 만료됐으면 키를 새로 만들지 않는다 (TTL 없는 음수 키 방지)
    private static final RedisScript<Long> REFUND = RedisScript.of("""
            if redis.call('EXISTS', KEYS[1]) == 1 then
              return redis.call('DECR', KEYS[1])
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final RateLimitConfig rateLimitConfig;

    public RateLimitServiceImpl(StringRedisTemplate redisTemplate, RateLimitConfig rateLimitConfig) {
        this.redisTemplate = redisTemplate;
        this.rateLimitConfig = rateLimitConfig;
    }

    @Override
    public void checkAndRecord(String subject, RateLimitAction action) {
        RateLimitConfig.ActionLimit config = getActionConfig(action);
        List<?> result = redisTemplate.execute(INCREMENT, List.of(buildKey(subject, action)),
                String.valueOf(config.getWindowSeconds()));
        long count = ((Number) result.get(0)).longValue();
        long ttl = ((Number) result.get(1)).longValue();

        if (count > config.getLimit()) {
            throw new RateLimitExceededException(
                    "Rate limit exceeded for " + action.name().toLowerCase(),
                    Math.max(1, ttl),
                    config.getLimit()
            );
        }
    }

    @Override
    public void refund(String subject, RateLimitAction action) {
        redisTemplate.execute(REFUND, List.of(buildKey(subject, action)));
    }

    private String buildKey(String subject, RateLimitAction action) {
        return KEY_PREFIX + action.name().toLowerCase() + ":" + subject;
    }

    private RateLimitConfig.ActionLimit getActionConfig(RateLimitAction action) {
        return switch (action) {
            case UPLOAD -> rateLimitConfig.getUpload();
            case COMMENT -> rateLimitConfig.getComment();
            case LIKE -> rateLimitConfig.getLike();
            case REPORT -> rateLimitConfig.getReport();
            case TAKEDOWN -> rateLimitConfig.getTakedown();
        };
    }
}
