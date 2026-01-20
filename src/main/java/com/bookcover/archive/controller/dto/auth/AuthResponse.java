package com.bookcover.archive.controller.dto.auth;

import java.util.UUID;

/**
 * 인증 응답 DTO.
 * 로그인 및 토큰 갱신 시 반환됩니다.
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserInfo user
) {
    public AuthResponse(String accessToken, String refreshToken, long expiresIn, UserInfo user) {
        this(accessToken, refreshToken, "Bearer", expiresIn, user);
    }

    public record UserInfo(
            UUID id,
            String email,
            String displayName,
            String role
    ) {
    }
}
