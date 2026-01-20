package com.bookcover.archive.service.dto;

import com.bookcover.archive.domain.collection.CollectionVisibility;

import java.util.UUID;

/**
 * 컬렉션 생성 요청 DTO.
 *
 * @param ownerId 소유자 ID
 * @param name 컬렉션 이름
 * @param description 컬렉션 설명 (선택)
 * @param visibility 공개 설정
 */
public record CreateCollectionRequest(
    UUID ownerId,
    String name,
    String description,
    CollectionVisibility visibility
) {}
