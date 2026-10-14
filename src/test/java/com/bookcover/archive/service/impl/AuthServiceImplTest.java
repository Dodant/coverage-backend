package com.bookcover.archive.service.impl;

import com.bookcover.archive.config.security.JwtProperties;
import com.bookcover.archive.config.security.JwtTokenProvider;
import com.bookcover.archive.controller.dto.auth.TokenRefreshRequest;
import com.bookcover.archive.domain.user.User;
import com.bookcover.archive.domain.user.UserRole;
import com.bookcover.archive.domain.user.UserStatus;
import com.bookcover.archive.exception.ValidationException;
import com.bookcover.archive.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 토큰 종류 분리와 리프레시 토큰 1회용·로그아웃 폐기를 확인한다. */
class AuthServiceImplTest {

    private static final UUID UID = UUID.randomUUID();
    private static final String SECRET = "test-secret-key-that-is-at-least-256-bits-long-for-hmac";

    private final JwtTokenProvider jwt = new JwtTokenProvider(props());
    private final UserRepository userRepository = mock(UserRepository.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> ops = mock(ValueOperations.class);
    private final AuthServiceImpl service = new AuthServiceImpl(userRepository, mock(PasswordEncoder.class), jwt,
            props(), mock(AuthenticationManager.class), redis);

    private static JwtProperties props() {
        JwtProperties p = new JwtProperties();
        p.setSecret(SECRET);
        p.setExpiration(60_000);
        p.setRefreshExpiration(120_000);
        return p;
    }

    private void activeUser(String email, UUID id) {
        userWithStatus(email, id, UserStatus.ACTIVE, null);
    }

    private void userWithStatus(String email, UUID id, UserStatus status, Instant suspendedUntil) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPasswordHash("x");
        user.setRole(UserRole.MEMBER);
        user.setStatus(status);
        user.setSuspendedUntil(suspendedUntil);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
    }

    @Test
    void 액세스와_리프레시는_서로_대신_쓸_수_없다() {
        String access = jwt.generateAccessToken("a@b.c", UID);
        String refresh = jwt.generateRefreshToken("a@b.c", UID);

        assertNotNull(jwt.parse(access, JwtTokenProvider.ACCESS));
        assertNull(jwt.parse(access, JwtTokenProvider.REFRESH));
        assertNotNull(jwt.parse(refresh, JwtTokenProvider.REFRESH));
        assertNull(jwt.parse(refresh, JwtTokenProvider.ACCESS));
    }

    @Test
    void 리프레시는_처음_한번만_성공한다() {
        activeUser("a@b.c", UID);
        when(redis.opsForValue()).thenReturn(ops);
        String refresh = jwt.generateRefreshToken("a@b.c", UID);

        when(ops.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        assertNotNull(service.refreshToken(new TokenRefreshRequest(refresh)));

        // 이미 폐기됨(로그아웃했거나 한 번 사용함)
        when(ops.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
        assertThrows(ValidationException.class, () -> service.refreshToken(new TokenRefreshRequest(refresh)));
    }

    @Test
    void 액세스_토큰으로는_갱신할_수_없다() {
        String access = jwt.generateAccessToken("a@b.c", UID);

        assertThrows(ValidationException.class, () -> service.refreshToken(new TokenRefreshRequest(access)));
        verifyNoInteractions(redis);
    }

    @Test
    void 로그아웃은_리프레시_jti를_폐기한다() {
        when(redis.opsForValue()).thenReturn(ops);
        String refresh = jwt.generateRefreshToken("a@b.c", UID);
        String jti = jwt.parse(refresh, JwtTokenProvider.REFRESH).getId();

        service.logout(refresh);

        verify(ops).setIfAbsent(eq("revoked_refresh:" + jti), eq("1"), any(Duration.class));
    }

    @Test
    void 잘못된_토큰으로_로그아웃해도_오류가_없다() {
        service.logout("garbage");
        verifyNoInteractions(redis);
    }

    @Test
    void 탈퇴_후_같은_이메일로_재가입한_계정에는_옛_리프레시가_통하지_않는다() {
        String oldRefresh = jwt.generateRefreshToken("a@b.c", UID);
        activeUser("a@b.c", UUID.randomUUID()); // 같은 이메일, 다른 계정
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        assertThrows(ValidationException.class, () -> service.refreshToken(new TokenRefreshRequest(oldRefresh)));
    }

    @Test
    void 정지_중이면_갱신을_거부하고_토큰을_소모하지_않는다() {
        userWithStatus("a@b.c", UID, UserStatus.SUSPENDED, Instant.now().plus(1, ChronoUnit.HOURS));
        when(redis.opsForValue()).thenReturn(ops);
        String refresh = jwt.generateRefreshToken("a@b.c", UID);

        assertThrows(ValidationException.class, () -> service.refreshToken(new TokenRefreshRequest(refresh)));
        verify(ops, never()).setIfAbsent(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void 사용자를_못_찾으면_토큰을_소모하지_않는다() {
        when(userRepository.findByEmail("a@b.c")).thenReturn(Optional.empty());
        when(redis.opsForValue()).thenReturn(ops);
        String refresh = jwt.generateRefreshToken("a@b.c", UID);

        assertThrows(ValidationException.class, () -> service.refreshToken(new TokenRefreshRequest(refresh)));
        verify(ops, never()).setIfAbsent(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void 정지_기간이_지났으면_갱신된다() {
        userWithStatus("a@b.c", UID, UserStatus.SUSPENDED, Instant.now().minus(1, ChronoUnit.MINUTES));
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        assertNotNull(service.refreshToken(new TokenRefreshRequest(jwt.generateRefreshToken("a@b.c", UID))));
    }

    @Test
    void 만료되었거나_typ이_없는_옛_토큰은_거부한다() {
        var key = Keys.hmacShaKeyFor(SECRET.getBytes(UTF_8));
        String legacy = Jwts.builder().subject("a@b.c")
                .expiration(new Date(System.currentTimeMillis() + 60_000)).signWith(key).compact();
        String expired = Jwts.builder().subject("a@b.c").claim("typ", JwtTokenProvider.ACCESS)
                .expiration(new Date(System.currentTimeMillis() - 1_000)).signWith(key).compact();

        assertNull(jwt.parse(legacy, JwtTokenProvider.ACCESS));
        assertNull(jwt.parse(expired, JwtTokenProvider.ACCESS));
    }
}
