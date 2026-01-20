package com.bookcover.archive.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * 리디자인 등록 요청 DTO.
 *
 * Requirements:
 * - 3.2: 리디자인 이미지, 설명을 필수 항목으로 검증
 * - 3.7: 리디자인이 등록되면 업로드한 회원 정보를 리디자인과 연결하여 저장
 */
public record CreateRedesignRequest(
    @NotBlank(message = "이미지 URL은 필수입니다")
    String imageUrl,

    @NotBlank(message = "썸네일 URL은 필수입니다")
    String thumbnailUrl,

    @NotBlank(message = "설명은 필수입니다")
    @Size(max = 2000, message = "설명은 2000자를 초과할 수 없습니다")
    String description,

    UUID originalBookId,

    UUID translatedBookId,

    @NotNull(message = "등록자 ID는 필수입니다")
    UUID createdById
) {
    /**
     * 원서 또는 번역서 중 하나는 반드시 지정되어야 합니다.
     */
    public boolean hasValidBookReference() {
        return originalBookId != null || translatedBookId != null;
    }
}
