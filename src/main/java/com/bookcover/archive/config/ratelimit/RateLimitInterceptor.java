package com.bookcover.archive.config.ratelimit;

import com.bookcover.archive.config.security.SecurityUtils;
import com.bookcover.archive.exception.RateLimitExceededException;
import com.bookcover.archive.service.RateLimitAction;
import com.bookcover.archive.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * 레이트 리밋 인터셉터.
 * @RateLimited 어노테이션이 적용된 컨트롤러 메서드에 레이트 리밋을 적용합니다.
 *
 * Requirements:
 * - 18.2: 동일 사용자가 짧은 시간 내 반복 신고를 하면 신고를 제한하고 캡차를 요구한다
 * - 18.3: 동일 사용자가 짧은 시간 내 대량 업로드를 시도하면 업로드를 제한한다
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;

    public RateLimitInterceptor(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RateLimited rateLimited = handlerMethod.getMethodAnnotation(RateLimited.class);
        if (rateLimited == null) {
            return true;
        }

        UUID userId = SecurityUtils.getCurrentUserId().orElse(null);
        if (userId == null) {
            // 인증되지 않은 사용자는 레이트 리밋 적용하지 않음
            return true;
        }

        // 사용자가 제한 상태인지 확인
        if (rateLimitService.isUserRestricted(userId)) {
            throw new RateLimitExceededException("User is restricted", 0, 0);
        }

        RateLimitAction action = rateLimited.action();
        rateLimitService.checkAndRecord(userId, action);

        return true;
    }
}
