package com.bookcover.archive.service;

import com.bookcover.archive.domain.collection.Collection;
import com.bookcover.archive.domain.collection.CollectionItemType;
import com.bookcover.archive.service.dto.CreateCollectionRequest;
import com.bookcover.archive.service.dto.UpdateCollectionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * 컬렉션 서비스 인터페이스.
 * 컬렉션 CRUD, 아이템 관리, 복제 기능을 제공합니다.
 *
 * Requirements:
 * - 11.1: 회원이 새 컬렉션을 생성하면 컬렉션 이름, 설명, 공개/비공개 설정을 저장한다
 * - 11.2: 회원이 북커버를 컬렉션에 추가하면 해당 북커버를 컬렉션과 연결한다
 * - 11.7: 회원이 컬렉션에서 북커버를 제거하면 해당 연결을 삭제한다
 * - 11.11: 다른 회원의 공개 컬렉션을 내 컬렉션으로 복제하는 기능을 제공한다
 */
public interface CollectionService {

    /**
     * 새 컬렉션을 생성합니다.
     *
     * @param request 컬렉션 생성 요청
     * @return 생성된 컬렉션
     */
    Collection createCollection(CreateCollectionRequest request);

    /**
     * 컬렉션 정보를 수정합니다.
     *
     * @param id 컬렉션 ID
     * @param userId 요청 회원 ID (소유자 확인용)
     * @param request 컬렉션 수정 요청
     * @return 수정된 컬렉션
     */
    Collection updateCollection(UUID id, UUID userId, UpdateCollectionRequest request);

    /**
     * 컬렉션을 삭제합니다.
     *
     * @param id 컬렉션 ID
     * @param userId 요청 회원 ID (소유자 확인용)
     */
    void deleteCollection(UUID id, UUID userId);

    /**
     * 컬렉션을 ID로 조회합니다.
     *
     * @param id 컬렉션 ID
     * @return 컬렉션 Optional
     */
    Optional<Collection> getCollection(UUID id);

    /**
     * 공유 토큰으로 컬렉션을 조회합니다.
     *
     * @param token 공유 토큰
     * @return 컬렉션 Optional
     */
    Optional<Collection> getCollectionByShareToken(String token);

    /**
     * 컬렉션에 아이템을 추가합니다.
     *
     * @param collectionId 컬렉션 ID
     * @param userId 요청 회원 ID (소유자 확인용)
     * @param itemType 아이템 유형
     * @param itemId 아이템 ID
     */
    void addItem(UUID collectionId, UUID userId, CollectionItemType itemType, UUID itemId);

    /**
     * 컬렉션에서 아이템을 제거합니다.
     *
     * @param collectionId 컬렉션 ID
     * @param userId 요청 회원 ID (소유자 확인용)
     * @param itemId 아이템 ID
     */
    void removeItem(UUID collectionId, UUID userId, UUID itemId);

    /**
     * 회원의 컬렉션 목록을 조회합니다.
     *
     * @param userId 회원 ID
     * @param pageable 페이징 정보
     * @return 컬렉션 페이지
     */
    Page<Collection> getUserCollections(UUID userId, Pageable pageable);

    /**
     * 공개 컬렉션 목록을 조회합니다.
     *
     * @param pageable 페이징 정보
     * @return 컬렉션 페이지
     */
    Page<Collection> getPublicCollections(Pageable pageable);

    /**
     * 컬렉션을 복제합니다.
     *
     * @param collectionId 원본 컬렉션 ID
     * @param newOwnerId 새 소유자 ID
     * @return 복제된 컬렉션
     */
    Collection duplicateCollection(UUID collectionId, UUID newOwnerId);
}
