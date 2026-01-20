package com.bookcover.archive.controller.dto.admin;

import java.util.Set;
import java.util.UUID;

/**
 * 별칭 목록 응답 DTO.
 */
public record AliasResponse(
        UUID entityId,
        String entityName,
        Set<String> aliases
) {
}
