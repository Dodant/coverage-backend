package com.bookcover.archive.service.dto;

import com.bookcover.archive.domain.book.CreditRole;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * 표지 크레딧 입력. designerId가 없으면 designerName으로 찾거나 새로 만든다.
 */
public record CreditInfo(
    UUID designerId,
    String designerName,

    @NotNull(message = "크레딧 역할은 필수입니다")
    CreditRole role
) {}
