package com.bookcover.archive.config.ratelimit;

import com.bookcover.archive.config.RateLimitConfig;
import com.bookcover.archive.config.security.CustomUserDetails;
import com.bookcover.archive.domain.user.User;
import com.bookcover.archive.domain.user.UserRole;
import com.bookcover.archive.exception.RateLimitExceededException;
import com.bookcover.archive.service.RateLimitAction;
import com.bookcover.archive.service.RateLimitService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class RateLimitInterceptorTest {

    private final RateLimitService service = mock(RateLimitService.class);
    private final RateLimitConfig config = new RateLimitConfig();
    private final RateLimitInterceptor interceptor = new RateLimitInterceptor(service, config);
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @SuppressWarnings("unused")
    static class Handlers {
        @RateLimited(action = RateLimitAction.COMMENT)
        public void limited() {}

        public void unlimited() {}
    }

    private static HandlerMethod handler(String name) throws NoSuchMethodException {
        return new HandlerMethod(new Handlers(), name);
    }

    private static UUID login() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(UserRole.MEMBER);
        CustomUserDetails principal = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        return user.getId();
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void countsLoggedInUserByUserIdNotAsAnonymous() throws Exception {
        UUID id = login();
        interceptor.preHandle(request, response, handler("limited"));
        verify(service).checkAndRecord("user:" + id, RateLimitAction.COMMENT);
    }

    @Test
    void skipsExemptUsers() throws Exception {
        UUID id = login();
        config.setExemptUserIds(Set.of(id));
        interceptor.preHandle(request, response, handler("limited"));
        response.setStatus(400);
        interceptor.afterCompletion(request, response, handler("limited"), null);
        verifyNoInteractions(service);
    }

    @Test
    void countsAnonymousRequestByIp() throws Exception {
        request.setRemoteAddr("203.0.113.7");
        interceptor.preHandle(request, response, handler("limited"));
        verify(service).checkAndRecord("ip:203.0.113.7", RateLimitAction.COMMENT);
    }

    @Test
    void skipsMethodsWithoutAnnotation() throws Exception {
        login();
        interceptor.preHandle(request, response, handler("unlimited"));
        interceptor.afterCompletion(request, response, handler("unlimited"), null);
        verifyNoInteractions(service);
    }

    @Test
    void refundsFailedRequests() throws Exception {
        UUID id = login();
        interceptor.preHandle(request, response, handler("limited"));
        response.setStatus(400);
        interceptor.afterCompletion(request, response, handler("limited"), null);
        verify(service).refund("user:" + id, RateLimitAction.COMMENT);
    }

    @Test
    void doesNotRefundServerErrors() throws Exception {
        login();
        interceptor.preHandle(request, response, handler("limited"));
        response.setStatus(500);
        interceptor.afterCompletion(request, response, handler("limited"), null);
        interceptor.afterCompletion(request, response, handler("limited"), new IllegalStateException());
        verify(service, never()).refund(any(), any());
    }

    @Test
    void keepsSuccessfulRequestsCounted() throws Exception {
        login();
        interceptor.preHandle(request, response, handler("limited"));
        response.setStatus(201);
        interceptor.afterCompletion(request, response, handler("limited"), null);
        verify(service, never()).refund(any(), any());
    }

    @Test
    void doesNotRefundWhenLimitRejectedTheRequest() throws Exception {
        login();
        doThrow(new RateLimitExceededException("limited", 10, 30)).when(service).checkAndRecord(any(), any());
        assertThrows(RateLimitExceededException.class,
                () -> interceptor.preHandle(request, response, handler("limited")));
        response.setStatus(429);
        interceptor.afterCompletion(request, response, handler("limited"), null);
        verify(service, never()).refund(any(), any());
    }
}
