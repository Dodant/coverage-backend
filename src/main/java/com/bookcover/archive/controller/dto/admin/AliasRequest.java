package com.bookcover.archive.controller.dto.admin;

import jakarta.validation.constraints.NotBlank;

/**
 * 별칭 추가/제거 요청 DTO.
 */
public record AliasRequest(
        @NotBlank(message = "Alias is required")
        String alias
) {
}
