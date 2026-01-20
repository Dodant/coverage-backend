package com.bookcover.archive.repository;

import com.bookcover.archive.domain.collection.CollectionItem;
import com.bookcover.archive.domain.collection.CollectionItemType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 컬렉션 아이템 Repository.
 * 컬렉션 아이템 엔티티에 대한 데이터 접근을 제공합니다.
 */
@Repository
public interface CollectionItemRepository extends JpaRepository<CollectionItem, UUID> {

    /**
     * 컬렉션 ID로 아이템 목록을 조회합니다.
     *
     * @param collectionId 컬렉션 ID
     * @return 아이템 목록
     */
    List<CollectionItem> findByCollectionId(UUID collectionId);

    /**
     * 컬렉션 ID로 아이템 목록을 페이징하여 조회합니다.
     *
     * @param collectionId 컬렉션 ID
     * @param pageable 페이징 정보
     * @return 아이템 페이지
     */
    Page<CollectionItem> findByCollectionId(UUID collectionId, Pageable pageable);

    /**
     * 컬렉션 ID와 아이템 ID로 아이템을 조회합니다.
     *
     * @param collectionId 컬렉션 ID
     * @param itemId 아이템 ID
     * @return 아이템 Optional
     */
    Optional<CollectionItem> findByCollectionIdAndItemId(UUID collectionId, UUID itemId);

    /**
     * 컬렉션 ID, 아이템 유형, 아이템 ID로 아이템을 조회합니다.
     *
     * @param collectionId 컬렉션 ID
     * @param itemType 아이템 유형
     * @param itemId 아이템 ID
     * @return 아이템 Optional
     */
    Optional<CollectionItem> findByCollectionIdAndItemTypeAndItemId(UUID collectionId, CollectionItemType itemType, UUID itemId);

    /**
     * 컬렉션에 특정 아이템이 존재하는지 확인합니다.
     *
     * @param collectionId 컬렉션 ID
     * @param itemType 아이템 유형
     * @param itemId 아이템 ID
     * @return 존재 여부
     */
    boolean existsByCollectionIdAndItemTypeAndItemId(UUID collectionId, CollectionItemType itemType, UUID itemId);

    /**
     * 컬렉션 ID와 아이템 유형으로 아이템 목록을 조회합니다.
     *
     * @param collectionId 컬렉션 ID
     * @param itemType 아이템 유형
     * @param pageable 페이징 정보
     * @return 아이템 페이지
     */
    Page<CollectionItem> findByCollectionIdAndItemType(UUID collectionId, CollectionItemType itemType, Pageable pageable);

    /**
     * 컬렉션 ID로 아이템 수를 조회합니다.
     *
     * @param collectionId 컬렉션 ID
     * @return 아이템 수
     */
    long countByCollectionId(UUID collectionId);

    /**
     * 컬렉션 ID와 아이템 유형으로 아이템 수를 조회합니다.
     *
     * @param collectionId 컬렉션 ID
     * @param itemType 아이템 유형
     * @return 아이템 수
     */
    long countByCollectionIdAndItemType(UUID collectionId, CollectionItemType itemType);

    /**
     * 컬렉션 ID와 아이템 ID로 아이템을 삭제합니다.
     *
     * @param collectionId 컬렉션 ID
     * @param itemId 아이템 ID
     */
    void deleteByCollectionIdAndItemId(UUID collectionId, UUID itemId);

    /**
     * 컬렉션 ID로 모든 아이템을 삭제합니다.
     *
     * @param collectionId 컬렉션 ID
     */
    void deleteByCollectionId(UUID collectionId);

    /**
     * 특정 아이템이 포함된 컬렉션 아이템 목록을 조회합니다.
     *
     * @param itemType 아이템 유형
     * @param itemId 아이템 ID
     * @return 컬렉션 아이템 목록
     */
    List<CollectionItem> findByItemTypeAndItemId(CollectionItemType itemType, UUID itemId);
}
