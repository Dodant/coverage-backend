package com.bookcover.archive.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 토큰 생성 및 검증을 담당하는 컴포넌트.
 *
 * Requirements: 7.1 - JWT 인증
 */
@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final String TYPE_CLAIM = "typ";
    private static final String USER_ID_CLAIM = "uid";
    public static final String ACCESS = "access";
    public static final String REFRESH = "refresh";

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 액세스 토큰을 생성합니다. subject는 이메일, uid 클레임은 사용자 ID.
     */
    public String generateAccessToken(String email, UUID userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getExpiration());

        return Jwts.builder()
                .subject(email)
                .claim(TYPE_CLAIM, ACCESS)
                .claim(USER_ID_CLAIM, userId.toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 리프레시 토큰을 생성합니다. subject는 이메일, uid 클레임은 사용자 ID, jti는 1회용 폐기 키.
     */
    public String generateRefreshToken(String email, UUID userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getRefreshExpiration());

        return Jwts.builder()
                .subject(email)
                .claim(TYPE_CLAIM, REFRESH)
                .claim(USER_ID_CLAIM, userId.toString())
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 서명·만료를 검증하고 토큰 종류가 일치하면 클레임을 반환합니다.
     * access/refresh를 서로 대신 쓰지 못하게 한다. 종류 클레임이 없는 옛 토큰은 거부한다(재로그인 필요).
     *
     * @return 유효하지 않거나 종류가 다르면 null
     */
    public Claims parse(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return expectedType.equals(claims.get(TYPE_CLAIM, String.class)) ? claims : null;
        } catch (SignatureException ex) {
            logger.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty");
        }
        return null;
    }

    /**
     * 토큰이 이 사용자에게 발급된 것인지 확인합니다.
     * subject(이메일)만 믿으면 탈퇴 후 같은 이메일로 재가입한 새 계정에 옛 토큰이 통한다.
     */
    public static boolean isIssuedTo(Claims claims, UUID userId) {
        return userId.toString().equals(claims.get(USER_ID_CLAIM, String.class));
    }
}
