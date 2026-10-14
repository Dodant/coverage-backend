package com.bookcover.archive.config.security;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bookcover.archive.domain.user.User;
import com.bookcover.archive.domain.user.UserRole;
import com.bookcover.archive.domain.user.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 모든 요청의 인증 여부를 정하는 필터 조건(토큰 종류·계정 상태·uid 일치)을 확인한다. */
class JwtAuthenticationFilterTest {

    private static final String EMAIL = "a@b.c";

    private final JwtTokenProvider jwt = new JwtTokenProvider(props());
    private final CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwt, userDetailsService);

    private static JwtProperties props() {
        JwtProperties p = new JwtProperties();
        p.setSecret("test-secret-key-that-is-at-least-256-bits-long-for-hmac");
        p.setExpiration(60_000);
        p.setRefreshExpiration(120_000);
        return p;
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private User user(UUID id, UserStatus status, Instant suspendedUntil) {
        User user = new User();
        user.setId(id);
        user.setEmail(EMAIL);
        user.setPasswordHash("x");
        user.setDisplayName("u");
        user.setRole(UserRole.MEMBER);
        user.setStatus(status);
        user.setSuspendedUntil(suspendedUntil);
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(new CustomUserDetails(user));
        return user;
    }

    private Authentication run(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Test
    void 정상_액세스_토큰은_인증된다() throws Exception {
        UUID id = UUID.randomUUID();
        user(id, UserStatus.ACTIVE, null);

        Authentication auth = run(jwt.generateAccessToken(EMAIL, id));

        assertNotNull(auth);
        assertEquals(id, ((CustomUserDetails) auth.getPrincipal()).getId());
    }

    @Test
    void 리프레시_토큰을_Bearer로_보내면_비로그인() throws Exception {
        UUID id = UUID.randomUUID();
        user(id, UserStatus.ACTIVE, null);

        assertNull(run(jwt.generateRefreshToken(EMAIL, id)));
    }

    @Test
    void 정지_중인_계정은_비로그인() throws Exception {
        UUID id = UUID.randomUUID();
        user(id, UserStatus.SUSPENDED, Instant.now().plus(1, ChronoUnit.HOURS));

        assertNull(run(jwt.generateAccessToken(EMAIL, id)));
    }

    @Test
    void 정지_기간이_지난_계정은_다시_인증된다() throws Exception {
        UUID id = UUID.randomUUID();
        user(id, UserStatus.SUSPENDED, Instant.now().minus(1, ChronoUnit.MINUTES));

        assertNotNull(run(jwt.generateAccessToken(EMAIL, id)));
    }

    @Test
    void 같은_이메일의_다른_계정이면_비로그인() throws Exception {
        user(UUID.randomUUID(), UserStatus.ACTIVE, null);

        assertNull(run(jwt.generateAccessToken(EMAIL, UUID.randomUUID())));
    }

    @Test
    void 탈퇴로_사용자를_못_찾아도_이메일을_ERROR_로그에_남기지_않는다() throws Exception {
        when(userDetailsService.loadUserByUsername(EMAIL))
                .thenThrow(new UsernameNotFoundException("User not found with email: " + EMAIL));
        ch.qos.logback.classic.Logger logger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(JwtAuthenticationFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            assertNull(run(jwt.generateAccessToken(EMAIL, UUID.randomUUID())));
        } finally {
            logger.detachAppender(appender);
        }

        assertTrue(appender.list.stream().noneMatch(e -> e.getLevel() == Level.ERROR
                || e.getFormattedMessage().contains(EMAIL)
                || (e.getThrowableProxy() != null && e.getThrowableProxy().getMessage().contains(EMAIL))));
    }
}
