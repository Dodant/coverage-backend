package com.bookcover.archive.service.impl;

import com.bookcover.archive.domain.collection.Collection;
import com.bookcover.archive.domain.collection.CollectionItem;
import com.bookcover.archive.domain.collection.CollectionItemType;
import com.bookcover.archive.domain.collection.CollectionVisibility;
import com.bookcover.archive.domain.user.User;
import com.bookcover.archive.exception.ResourceNotFoundException;
import com.bookcover.archive.exception.ValidationException;
import com.bookcover.archive.repository.CollectionItemRepository;
import com.bookcover.archive.repository.CollectionRepository;
import com.bookcover.archive.repository.UserRepository;
import com.bookcover.archive.service.CollectionService;
import com.bookcover.archive.service.dto.CreateCollectionRequest;
import com.bookcover.archive.service.dto.UpdateCollectionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 컬렉션 서비스 구현체.
 * 컬렉션 CRUD, 아이템 관리, 복제 기능을 구현합니다.
 */
@Service
@Transactional
public class CollectionServiceImpl implements CollectionService {

    private final CollectionRepository collectionRepository;
    private final CollectionItemRepository collectionItemRepository;
    private final UserRepository userRepository;

    public CollectionServiceImpl(
            CollectionRepository collectionRepository,
            CollectionItemRepository collectionItemRepository,
            UserRepository userRepository) {
        this.collectionRepository = collectionRepository;
        this.collectionItemRepository = collectionItemRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Collection createCollection(CreateCollectionRequest request) {
        // 이름 검증
        if (request.name() == null || request.name().isBlank()) {
            throw new ValidationException("Collection name cannot be empty",
                    Map.of("name", request.name() != null ? request.name() : "null"));
        }

        User owner = userRepository.findById(request.ownerId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.ownerId()));

        Collection collection = new Collection();
        collection.setOwner(owner);
        collection.setName(request.name());
        collection.setDescription(request.description());
        collection.setVisibility(request.visibility() != null ? request.visibility() : CollectionVisibility.PRIVATE);
        collection.setShareToken(UUID.randomUUID().toString());
        collection.setCreatedAt(Instant.now());
        collection.setUpdatedAt(Instant.now());

        return collectionRepository.save(collection);
    }

    @Override
    public Collection updateCollection(UUID id, UUID userId, UpdateCollectionRequest request) {
        Collection collection = collectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collection", id));

        // 소유자 확인
        validateOwnership(collection, userId);

        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw new ValidationException("Collection name cannot be empty",
                        Map.of("name", request.name()));
            }
            collection.setName(request.name());
        }

        if (request.description() != null) {
            collection.setDescription(request.description());
        }

        if (request.visibility() != null) {
            collection.setVisibility(request.visibility());
        }

        collection.setUpdatedAt(Instant.now());

        return collectionRepository.save(collection);
    }

    @Override
    public void deleteCollection(UUID id, UUID userId) {
        Collection collection = collectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collection", id));

        // 소유자 확인
        validateOwnership(collection, userId);

        collectionRepository.delete(collection);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Collection> getCollection(UUID id) {
        return collectionRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Collection> getCollectionByShareToken(String token) {
        return collectionRepository.findByShareToken(token);
    }

    @Override
    public void addItem(UUID collectionId, UUID userId, CollectionItemType itemType, UUID itemId) {
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection", collectionId));

        // 소유자 확인
        validateOwnership(collection, userId);

        // 중복 확인
        if (collectionItemRepository.existsByCollectionIdAndItemTypeAndItemId(collectionId, itemType, itemId)) {
            throw new ValidationException("Item already exists in collection",
                    Map.of("collectionId", collectionId, "itemType", itemType, "itemId", itemId));
        }

        CollectionItem item = new CollectionItem();
        item.setCollection(collection);
        item.setItemType(itemType);
        item.setItemId(itemId);
        item.setAddedAt(Instant.now());

        collectionItemRepository.save(item);

        collection.setUpdatedAt(Instant.now());
        collectionRepository.save(collection);
    }

    @Override
    public void removeItem(UUID collectionId, UUID userId, UUID itemId) {
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection", collectionId));

        // 소유자 확인
        validateOwnership(collection, userId);

        CollectionItem item = collectionItemRepository.findByCollectionIdAndItemId(collectionId, itemId)
                .orElseThrow(() -> new ResourceNotFoundException("CollectionItem", itemId));

        collectionItemRepository.delete(item);

        collection.setUpdatedAt(Instant.now());
        collectionRepository.save(collection);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Collection> getUserCollections(UUID userId, Pageable pageable) {
        return collectionRepository.findByOwnerId(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Collection> getPublicCollections(Pageable pageable) {
        return collectionRepository.findByVisibility(CollectionVisibility.PUBLIC, pageable);
    }

    @Override
    public Collection duplicateCollection(UUID collectionId, UUID newOwnerId) {
        Collection original = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection", collectionId));

        // 공개 컬렉션 또는 링크 공유 컬렉션만 복제 가능
        if (original.getVisibility() == CollectionVisibility.PRIVATE &&
                !original.getOwner().getId().equals(newOwnerId)) {
            throw new ValidationException("Cannot duplicate private collection",
                    Map.of("collectionId", collectionId));
        }

        User newOwner = userRepository.findById(newOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", newOwnerId));

        // 새 컬렉션 생성
        Collection duplicate = new Collection();
        duplicate.setOwner(newOwner);
        duplicate.setName(original.getName() + " (복제)");
        duplicate.setDescription(original.getDescription());
        duplicate.setVisibility(CollectionVisibility.PRIVATE);
        duplicate.setShareToken(UUID.randomUUID().toString());
        duplicate.setCreatedAt(Instant.now());
        duplicate.setUpdatedAt(Instant.now());

        Collection savedDuplicate = collectionRepository.save(duplicate);

        // 아이템 복제
        for (CollectionItem originalItem : original.getItems()) {
            CollectionItem duplicateItem = new CollectionItem();
            duplicateItem.setCollection(savedDuplicate);
            duplicateItem.setItemType(originalItem.getItemType());
            duplicateItem.setItemId(originalItem.getItemId());
            duplicateItem.setAddedAt(Instant.now());
            collectionItemRepository.save(duplicateItem);
        }

        return savedDuplicate;
    }

    // Private helper methods

    private void validateOwnership(Collection collection, UUID userId) {
        if (!collection.getOwner().getId().equals(userId)) {
            throw new ValidationException("Only the owner can modify this collection",
                    Map.of("collectionId", collection.getId(), "userId", userId));
        }
    }
}
