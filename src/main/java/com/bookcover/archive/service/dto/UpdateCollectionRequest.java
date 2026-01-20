package com.bookcover.archive.service.dto;

import com.bookcover.archive.domain.collection.CollectionVisibility;

/**
 * 컬렉션 수정 요청 DTO.
 *
 * @param name 컬렉션 이름 (null이면 변경 안함)
 * @param description 컬렉션 설명 (null이면 변경 안함)
 * @param visibility 공개 설정 (null이면 변경 안함)
 */
public record UpdateCollectionRequest(
    String name,
    String description,
    CollectionVisibility visibility
) {}
