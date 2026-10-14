package com.bookcover.archive.config.ratelimit;

import com.bookcover.archive.config.security.SecurityUtils;
import com.bookcover.archive.service.RateLimitAction;
import com.bookcover.archive.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 레이트 리밋 인터셉터.
 * @RateLimited 어노테이션이 적용된 컨트롤러 메서드에 레이트 리밋을 적용합니다.
 * 로그인 사용자는 사용자 ID, 비로그인 요청은 IP 단위로 셉니다.
 * 실패(4xx/5xx)로 끝난 요청은 횟수를 되돌려, 검증에 걸린 요청이 한도를 깎지 않게 합니다.
 *
 * Requirements:
 * - 18.2: 동일 사용자가 짧은 시간 내 반복 신고를 하면 신고를 제한하고 캡차를 요구한다
 * - 18.3: 동일 사용자가 짧은 시간 내 대량 업로드를 시도하면 업로드를 제한한다
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final String RECORDED_ATTR = RateLimitInterceptor.class.getName() + ".recorded";

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

        // 프록시 뒤에서는 server.forward-headers-strategy 설정이 있어야 getRemoteAddr가 실제 클라이언트 IP다
        String subject = SecurityUtils.getCurrentUserId()
                .map(id -> "user:" + id)
                .orElseGet(() -> "ip:" + request.getRemoteAddr());

        rateLimitService.checkAndRecord(subject, rateLimited.action());
        request.setAttribute(RECORDED_ATTR, new Recorded(subject, rateLimited.action()));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (request.getAttribute(RECORDED_ATTR) instanceof Recorded recorded
                && (ex != null || response.getStatus() >= 400)) {
            rateLimitService.refund(recorded.subject(), recorded.action());
        }
    }

    private record Recorded(String subject, RateLimitAction action) {}
}
