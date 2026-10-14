package com.bookcover.archive.service.impl;

import com.bookcover.archive.config.security.JwtProperties;
import com.bookcover.archive.config.security.JwtTokenProvider;
import com.bookcover.archive.controller.dto.auth.TokenRefreshRequest;
import com.bookcover.archive.domain.user.User;
import com.bookcover.archive.domain.user.UserRole;
import com.bookcover.archive.domain.user.UserStatus;
import com.bookcover.archive.exception.ValidationException;
import com.bookcover.archive.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 토큰 종류 분리와 리프레시 토큰 1회용·로그아웃 폐기를 확인한다. */
class AuthServiceImplTest {

    private final JwtTokenProvider jwt = new JwtTokenProvider(props());
    private final UserRepository userRepository = mock(UserRepository.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> ops = mock(ValueOperations.class);
    private final AuthServiceImpl service = new AuthServiceImpl(userRepository, mock(PasswordEncoder.class), jwt,
            props(), mock(AuthenticationManager.class), redis);

    private static JwtProperties props() {
        JwtProperties p = new JwtProperties();
        p.setSecret("test-secret-key-that-is-at-least-256-bits-long-for-hmac");
        p.setExpiration(60_000);
        p.setRefreshExpiration(120_000);
        return p;
    }

    private void activeUser(String email) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setRole(UserRole.MEMBER);
        user.setStatus(UserStatus.ACTIVE);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
    }

    @Test
    void 액세스와_리프레시는_서로_대신_쓸_수_없다() {
        String access = jwt.generateAccessToken("a@b.c");
        String refresh = jwt.generateRefreshToken("a@b.c");

        assertNotNull(jwt.parse(access, JwtTokenProvider.ACCESS));
        assertNull(jwt.parse(access, JwtTokenProvider.REFRESH));
        assertNotNull(jwt.parse(refresh, JwtTokenProvider.REFRESH));
        assertNull(jwt.parse(refresh, JwtTokenProvider.ACCESS));
    }

    @Test
    void 리프레시는_처음_한번만_성공한다() {
        activeUser("a@b.c");
        when(redis.opsForValue()).thenReturn(ops);
        String refresh = jwt.generateRefreshToken("a@b.c");

        when(ops.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        assertNotNull(service.refreshToken(new TokenRefreshRequest(refresh)));

        // 이미 폐기됨(로그아웃했거나 한 번 사용함)
        when(ops.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
        assertThrows(ValidationException.class, () -> service.refreshToken(new TokenRefreshRequest(refresh)));
    }

    @Test
    void 액세스_토큰으로는_갱신할_수_없다() {
        String access = jwt.generateAccessToken("a@b.c");

        assertThrows(ValidationException.class, () -> service.refreshToken(new TokenRefreshRequest(access)));
        verifyNoInteractions(redis);
    }

    @Test
    void 로그아웃은_리프레시_jti를_폐기한다() {
        when(redis.opsForValue()).thenReturn(ops);
        String refresh = jwt.generateRefreshToken("a@b.c");
        String jti = jwt.parse(refresh, JwtTokenProvider.REFRESH).getId();

        service.logout(refresh);

        verify(ops).setIfAbsent(eq("revoked_refresh:" + jti), eq("1"), any(Duration.class));
    }

    @Test
    void 잘못된_토큰으로_로그아웃해도_오류가_없다() {
        service.logout("garbage");
        verifyNoInteractions(redis);
    }
}
