package com.bookcover.archive.controller.dto.auth;

import com.bookcover.archive.domain.user.UserRole;
import com.bookcover.archive.validation.annotation.ValidUrl;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * 회원가입 요청 DTO.
 *
 * Requirements: 7.1 - 회원가입 시 역할 선택
 */
public record SignupRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String password,

        @NotBlank(message = "Display name is required")
        @Size(min = 2, max = 50, message = "Display name must be between 2 and 50 characters")
        String displayName,

        @NotNull(message = "Role is required")
        UserRole role,

        @ValidUrl
        String websiteUrl,

        @ValidUrl
        String portfolioUrl,

        Map<String, String> snsAccounts
) {
}
