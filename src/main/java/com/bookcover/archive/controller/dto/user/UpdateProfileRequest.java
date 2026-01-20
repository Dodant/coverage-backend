package com.bookcover.archive.controller.dto.user;

import com.bookcover.archive.validation.annotation.ValidUrl;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * 프로필 수정 요청 DTO.
 *
 * Requirements: 7.8 - 회원이 프로필 정보를 수정할 수 있게 함
 */
public record UpdateProfileRequest(
        @Size(min = 2, max = 50, message = "Display name must be between 2 and 50 characters")
        String displayName,

        @ValidUrl
        String websiteUrl,

        @ValidUrl
        String portfolioUrl,

        Map<String, String> snsAccounts
) {
}
