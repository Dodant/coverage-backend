package com.bookcover.archive.service.impl

import com.bookcover.archive.domain.collection.Collection
import com.bookcover.archive.domain.collection.CollectionItem
import com.bookcover.archive.domain.collection.CollectionItemType
import com.bookcover.archive.domain.collection.CollectionVisibility
import com.bookcover.archive.exception.ResourceNotFoundException
import com.bookcover.archive.exception.UnauthorizedException
import com.bookcover.archive.exception.ValidationException
import com.bookcover.archive.repository.collection.CollectionItemRepository
import com.bookcover.archive.repository.collection.CollectionRepository
import com.bookcover.archive.service.CollectionService
import com.bookcover.archive.service.dto.CreateCollectionRequest
import com.bookcover.archive.service.dto.UpdateCollectionRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Implementation of CollectionService for collection management.
 *
 * Requirements:
 * - 11.1: Collection creation with name, description, visibility
 * - 11.2: Add book covers to collections
 * - 11.7: Remove items from collections
 * - 11.11: Duplicate collections
 */
@Service
@Transactional
class CollectionServiceImpl(
    private val collectionRepository: CollectionRepository,
    private val collectionItemRepository: CollectionItemRepository
) : CollectionService {

    // ==================== Collection CRUD ====================

    /**
     * Creates a new collection.
     * Requirement: 11.1
     */
    override fun createCollection(request: CreateCollectionRequest): Collection {
        validateCollectionName(request.name)

        val collection = Collection(
            name = request.name.trim(),
            description = request.description?.trim(),
            visibility = request.visibility,
            ownerId = request.ownerId
        )

        return collectionRepository.save(collection)
    }

    /**
     * Updates an existing collection.
     */
    override fun updateCollection(collectionId: UUID, request: UpdateCollectionRequest, userId: UUID): Collection {
        val collection = collectionRepository.findById(collectionId).orElseThrow {
            ResourceNotFoundException("Collection", collectionId)
        }

        // Check authorization
        if (collection.ownerId != userId) {
            throw UnauthorizedException("You can only update your own collections")
        }

        request.name?.let { name ->
            validateCollectionName(name)
            collection.name = name.trim()
        }

        request.description?.let { description ->
            collection.description = description.trim()
        }

        request.visibility?.let { visibility ->
            collection.visibility = visibility
        }

        return collectionRepository.save(collection)
    }

    /**
     * Deletes a collection.
     */
    override fun deleteCollection(collectionId: UUID, userId: UUID) {
        val collection = collectionRepository.findById(collectionId).orElseThrow {
            ResourceNotFoundException("Collection", collectionId)
        }

        // Check authorization
        if (collection.ownerId != userId) {
            throw UnauthorizedException("You can only delete your own collections")
        }

        // Delete all items first (handled by cascade, but explicit for clarity)
        collectionItemRepository.deleteByCollectionId(collectionId)
        collectionRepository.delete(collection)
    }

    /**
     * Gets a collection by ID.
     */
    @Transactional(readOnly = true)
    override fun getCollection(collectionId: UUID): Collection? {
        return collectionRepository.findById(collectionId).orElse(null)
    }

    /**
     * Gets a collection by share token.
     */
    @Transactional(readOnly = true)
    override fun getCollectionByShareToken(shareToken: String): Collection? {
        return collectionRepository.findByShareToken(shareToken)
    }

    // ==================== Item Management ====================

    /**
     * Adds an item to a collection.
     * Requirement: 11.2
     */
    override fun addItem(collectionId: UUID, itemType: CollectionItemType, itemId: UUID, userId: UUID): CollectionItem {
        val collection = collectionRepository.findById(collectionId).orElseThrow {
            ResourceNotFoundException("Collection", collectionId)
        }

        // Check authorization
        if (collection.ownerId != userId) {
            throw UnauthorizedException("You can only add items to your own collections")
        }

        // Check if item already exists in collection
        if (collectionItemRepository.existsByCollectionIdAndItemTypeAndItemId(collectionId, itemType, itemId)) {
            throw ValidationException(
                "Item already exists in collection",
                mapOf("itemId" to "This item is already in the collection")
            )
        }

        val item = collection.addItem(itemType, itemId)
        collectionRepository.save(collection)

        return item
    }

    /**
     * Removes an item from a collection by CollectionItem ID.
     * Requirement: 11.7
     */
    override fun removeItem(collectionId: UUID, itemId: UUID, userId: UUID) {
        val collection = collectionRepository.findById(collectionId).orElseThrow {
            ResourceNotFoundException("Collection", collectionId)
        }

        // Check authorization
        if (collection.ownerId != userId) {
            throw UnauthorizedException("You can only remove items from your own collections")
        }

        val removed = collection.removeItem(itemId)
        if (!removed) {
            throw ResourceNotFoundException("CollectionItem", itemId)
        }

        collectionRepository.save(collection)
    }

    /**
     * Removes an item from a collection by item type and item ID.
     * Requirement: 11.7
     */
    override fun removeItemByTypeAndId(collectionId: UUID, itemType: CollectionItemType, itemId: UUID, userId: UUID) {
        val collection = collectionRepository.findById(collectionId).orElseThrow {
            ResourceNotFoundException("Collection", collectionId)
        }

        // Check authorization
        if (collection.ownerId != userId) {
            throw UnauthorizedException("You can only remove items from your own collections")
        }

        val item = collectionItemRepository.findByCollectionIdAndItemTypeAndItemId(collectionId, itemType, itemId)
            ?: throw ResourceNotFoundException("CollectionItem", itemId)

        collection.items.remove(item)
        collectionRepository.save(collection)
    }

    // ==================== Query Operations ====================

    /**
     * Gets all collections owned by a user.
     */
    @Transactional(readOnly = true)
    override fun getUserCollections(userId: UUID, pageable: Pageable): Page<Collection> {
        return collectionRepository.findByOwnerId(userId, pageable)
    }

    /**
     * Gets all public collections.
     */
    @Transactional(readOnly = true)
    override fun getPublicCollections(pageable: Pageable): Page<Collection> {
        return collectionRepository.findByVisibility(CollectionVisibility.PUBLIC, pageable)
    }

    /**
     * Gets items in a collection.
     */
    @Transactional(readOnly = true)
    override fun getCollectionItems(collectionId: UUID, pageable: Pageable): Page<CollectionItem> {
        return collectionItemRepository.findByCollectionId(collectionId, pageable)
    }

    // ==================== Duplication ====================

    /**
     * Duplicates a collection to a new owner.
     * Requirement: 11.11
     */
    override fun duplicateCollection(collectionId: UUID, newOwnerId: UUID): Collection {
        val sourceCollection = collectionRepository.findById(collectionId).orElseThrow {
            ResourceNotFoundException("Collection", collectionId)
        }

        // Check if the collection is accessible for duplication
        // Only public collections or link-only collections can be duplicated by others
        if (sourceCollection.ownerId != newOwnerId &&
            sourceCollection.visibility == CollectionVisibility.PRIVATE) {
            throw UnauthorizedException("Cannot duplicate a private collection")
        }

        // Create a new collection with the same name and description
        val newCollection = Collection(
            name = "${sourceCollection.name} (Copy)",
            description = sourceCollection.description,
            visibility = CollectionVisibility.PRIVATE, // New collection starts as private
            ownerId = newOwnerId
        )

        val savedCollection = collectionRepository.save(newCollection)

        // Copy all items from the source collection
        val sourceItems = collectionItemRepository.findByCollectionId(collectionId)
        for (sourceItem in sourceItems) {
            savedCollection.addItem(sourceItem.itemType, sourceItem.itemId)
        }

        return collectionRepository.save(savedCollection)
    }

    /**
     * Checks if a collection contains a specific item.
     */
    @Transactional(readOnly = true)
    override fun containsItem(collectionId: UUID, itemType: CollectionItemType, itemId: UUID): Boolean {
        return collectionItemRepository.existsByCollectionIdAndItemTypeAndItemId(collectionId, itemType, itemId)
    }

    /**
     * Validates collection name.
     */
    private fun validateCollectionName(name: String) {
        if (name.isBlank()) {
            throw ValidationException(
                "Collection name cannot be blank",
                mapOf("name" to "Collection name is required")
            )
        }
    }
}
