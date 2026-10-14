package com.bookcover.archive.service.impl;

import com.bookcover.archive.config.security.JwtProperties;
import com.bookcover.archive.config.security.JwtTokenProvider;
import com.bookcover.archive.controller.dto.auth.AuthResponse;
import com.bookcover.archive.controller.dto.auth.LoginRequest;
import com.bookcover.archive.controller.dto.auth.SignupRequest;
import com.bookcover.archive.controller.dto.auth.TokenRefreshRequest;
import com.bookcover.archive.domain.user.User;
import com.bookcover.archive.domain.user.UserRole;
import com.bookcover.archive.domain.user.UserStatus;
import com.bookcover.archive.domain.user.UserTitle;
import com.bookcover.archive.exception.ValidationException;
import com.bookcover.archive.repository.UserRepository;
import com.bookcover.archive.service.AuthService;
import io.jsonwebtoken.Claims;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 인증 서비스 구현체.
 *
 * Requirements: 7.1, 7.8
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final String REVOKED_KEY_PREFIX = "revoked_refresh:";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final AuthenticationManager authenticationManager;
    private final StringRedisTemplate redisTemplate;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider,
                           JwtProperties jwtProperties,
                           AuthenticationManager authenticationManager,
                           StringRedisTemplate redisTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
        this.authenticationManager = authenticationManager;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        // 이메일 중복 검사
        if (userRepository.existsByEmail(request.email())) {
            throw new ValidationException("Email already exists", Map.of("email", "Email is already registered"));
        }

        // ADMIN 역할은 회원가입으로 생성 불가
        if (request.role() == UserRole.ADMIN) {
            throw new ValidationException("Invalid role", Map.of("role", "Cannot register as ADMIN"));
        }

        // 새 사용자 생성
        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName());
        user.setRole(request.role());
        user.setTitle(UserTitle.MEMBER);
        user.setStatus(UserStatus.ACTIVE);
        user.setContributionCount(0);
        user.setReportRestricted(false);
        user.setCreatedAt(Instant.now());

        if (request.websiteUrl() != null && !request.websiteUrl().isBlank()) {
            user.setWebsiteUrl(request.websiteUrl());
        }
        if (request.portfolioUrl() != null && !request.portfolioUrl().isBlank()) {
            user.setPortfolioUrl(request.portfolioUrl());
        }
        if (request.snsAccounts() != null && !request.snsAccounts().isEmpty()) {
            user.setSnsAccounts(new HashMap<>(request.snsAccounts()));
        }

        user = userRepository.save(user);

        // 토큰 생성
        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        return createAuthResponse(user, accessToken, refreshToken);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );

            User user = userRepository.findByEmail(request.email())
                    .orElseThrow(() -> new ValidationException("Invalid credentials",
                            Map.of("credentials", "Invalid email or password")));

            // 계정 상태 확인
            if (user.getStatus() == UserStatus.DELETED) {
                throw new ValidationException("Account deleted", Map.of("account", "This account has been deleted"));
            }
            if (user.getStatus() == UserStatus.SUSPENDED) {
                if (user.getSuspendedUntil() != null && Instant.now().isBefore(user.getSuspendedUntil())) {
                    throw new ValidationException("Account suspended",
                            Map.of("account", "This account is suspended until " + user.getSuspendedUntil()));
                } else {
                    // 문제 12 해결: 정지 기간이 지났으면 자동으로 상태 복원
                    user.setStatus(UserStatus.ACTIVE);
                    user.setSuspendedUntil(null);
                }
            }

            // 마지막 로그인 시간 업데이트
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);

            // 토큰 생성
            String accessToken = jwtTokenProvider.generateAccessToken(authentication);
            String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

            return createAuthResponse(user, accessToken, refreshToken);
        } catch (BadCredentialsException e) {
            throw new ValidationException("Invalid credentials", Map.of("credentials", "Invalid email or password"));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(TokenRefreshRequest request) {
        Claims claims = jwtTokenProvider.parse(request.refreshToken(), JwtTokenProvider.REFRESH);

        // 리프레시 토큰은 1회용: 사용과 동시에 폐기해 탈취된 옛 토큰·로그아웃한 토큰을 막는다
        if (claims == null || !revoke(claims)) {
            throw new ValidationException("Invalid refresh token", Map.of("refreshToken", "Refresh token is invalid or expired"));
        }

        String email = claims.getSubject();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ValidationException("User not found", Map.of("user", "User not found")));

        // 계정 상태 확인
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ValidationException("Account not active", Map.of("account", "Account is not active"));
        }

        // 새 토큰 생성
        String newAccessToken = jwtTokenProvider.generateAccessToken(email);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(email);

        return createAuthResponse(user, newAccessToken, newRefreshToken);
    }

    @Override
    public void logout(String refreshToken) {
        Claims claims = jwtTokenProvider.parse(refreshToken, JwtTokenProvider.REFRESH);
        if (claims != null) {
            revoke(claims);
        }
    }

    /**
     * 리프레시 토큰의 jti를 남은 유효기간 동안 Redis에 폐기 표시한다.
     *
     * @return 이번 호출로 폐기했으면 true, 이미 폐기된 토큰이면 false (동시 갱신 시 한 요청만 성공)
     */
    private boolean revoke(Claims claims) {
        long ttlMillis = claims.getExpiration().getTime() - System.currentTimeMillis();
        if (ttlMillis <= 0 || claims.getId() == null) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.opsForValue()
                .setIfAbsent(REVOKED_KEY_PREFIX + claims.getId(), "1", Duration.ofMillis(ttlMillis)));
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ValidationException("User not found", Map.of("email", "User not found with email: " + email)));
    }

    private AuthResponse createAuthResponse(User user, String accessToken, String refreshToken) {
        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole().name()
        );

        return new AuthResponse(
                accessToken,
                refreshToken,
                jwtProperties.getExpiration() / 1000, // Convert to seconds
                userInfo
        );
    }
}
