package com.bookcover.archive.service.dto;

import com.bookcover.archive.domain.book.CoverVariantType;
import com.bookcover.archive.domain.book.RibbonStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import com.bookcover.archive.domain.book.CreditRole;

/** 표지 메타데이터 교체. 저작권자·크레딧은 null/생략이면 유지한다. 저작권자는 빈 문자열, 크레딧은 빈 목록으로 비운다. */
public record UpdateCoverMetadataRequest(
        @Size(max = 100, message = "설명은 100자 이하여야 합니다") String label,
        @Positive(message = "쇄는 1 이상이어야 합니다") Integer printingFrom,
        LocalDate usedFrom,
        @NotNull CoverVariantType variantType,
        @NotNull RibbonStatus ribbonStatus,
        @Size(max = 100, message = "출처 이름은 100자 이하여야 합니다") String sourceName,
        @Size(max = 1000, message = "출처 URL은 1000자 이하여야 합니다")
        @Pattern(regexp = "(?i)^(https?://[^\\s]+)?$", message = "출처는 http(s) URL이어야 합니다") String sourceUrl,
        @Size(max = 20, message = "크레딧은 최대 20개까지 등록할 수 있습니다") List<@NotNull @Valid CreditAssignment> credits,
        @Size(max = 500, message = "표지 저작권자는 500자 이하여야 합니다") String copyrightHolder
) {
    public record CreditAssignment(@NotNull UUID designerId, @NotNull CreditRole role) {}
}
