package com.bookcover.archive.controller.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * 토큰 갱신 요청 DTO.
 */
public record TokenRefreshRequest(
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {
}
