package com.bookcover.archive.service

import com.bookcover.archive.domain.collection.Collection
import com.bookcover.archive.domain.collection.CollectionItem
import com.bookcover.archive.domain.collection.CollectionItemType
import com.bookcover.archive.service.dto.CreateCollectionRequest
import com.bookcover.archive.service.dto.UpdateCollectionRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

/**
 * Service interface for collection management.
 *
 * Requirements:
 * - 11.1: Collection creation with name, description, visibility
 * - 11.2: Add book covers to collections
 * - 11.7: Remove items from collections
 * - 11.11: Duplicate collections
 */
interface CollectionService {

    // ==================== Collection CRUD ====================

    /**
     * Creates a new collection.
     * Requirement: 11.1
     *
     * @param request The collection creation request
     * @return The created collection
     * @throws ValidationException if name is blank
     */
    fun createCollection(request: CreateCollectionRequest): Collection

    /**
     * Updates an existing collection.
     *
     * @param collectionId The ID of the collection to update
     * @param request The update request
     * @param userId The ID of the user making the update (for authorization)
     * @return The updated collection
     * @throws ResourceNotFoundException if collection not found
     * @throws UnauthorizedException if user is not the owner
     */
    fun updateCollection(collectionId: UUID, request: UpdateCollectionRequest, userId: UUID): Collection

    /**
     * Deletes a collection.
     *
     * @param collectionId The ID of the collection to delete
     * @param userId The ID of the user making the deletion (for authorization)
     * @throws ResourceNotFoundException if collection not found
     * @throws UnauthorizedException if user is not the owner
     */
    fun deleteCollection(collectionId: UUID, userId: UUID)

    /**
     * Gets a collection by ID.
     *
     * @param collectionId The collection ID
     * @return The collection or null if not found
     */
    fun getCollection(collectionId: UUID): Collection?

    /**
     * Gets a collection by share token.
     *
     * @param shareToken The share token
     * @return The collection or null if not found
     */
    fun getCollectionByShareToken(shareToken: String): Collection?

    // ==================== Item Management ====================

    /**
     * Adds an item to a collection.
     * Requirement: 11.2
     *
     * @param collectionId The ID of the collection
     * @param itemType The type of item to add
     * @param itemId The ID of the item
     * @param userId The ID of the user making the addition (for authorization)
     * @return The created collection item
     * @throws ResourceNotFoundException if collection not found
     * @throws UnauthorizedException if user is not the owner
     * @throws ValidationException if item already exists in collection
     */
    fun addItem(collectionId: UUID, itemType: CollectionItemType, itemId: UUID, userId: UUID): CollectionItem

    /**
     * Removes an item from a collection.
     * Requirement: 11.7
     *
     * @param collectionId The ID of the collection
     * @param itemId The ID of the item to remove
     * @param userId The ID of the user making the removal (for authorization)
     * @throws ResourceNotFoundException if collection or item not found
     * @throws UnauthorizedException if user is not the owner
     */
    fun removeItem(collectionId: UUID, itemId: UUID, userId: UUID)

    /**
     * Removes an item from a collection by item type and item ID.
     * Requirement: 11.7
     *
     * @param collectionId The ID of the collection
     * @param itemType The type of item
     * @param itemId The ID of the item
     * @param userId The ID of the user making the removal (for authorization)
     * @throws ResourceNotFoundException if collection or item not found
     * @throws UnauthorizedException if user is not the owner
     */
    fun removeItemByTypeAndId(collectionId: UUID, itemType: CollectionItemType, itemId: UUID, userId: UUID)

    // ==================== Query Operations ====================

    /**
     * Gets all collections owned by a user.
     *
     * @param userId The owner's ID
     * @param pageable Pagination information
     * @return Page of collections
     */
    fun getUserCollections(userId: UUID, pageable: Pageable): Page<Collection>

    /**
     * Gets all public collections.
     *
     * @param pageable Pagination information
     * @return Page of public collections
     */
    fun getPublicCollections(pageable: Pageable): Page<Collection>

    /**
     * Gets items in a collection.
     *
     * @param collectionId The collection ID
     * @param pageable Pagination information
     * @return Page of collection items
     */
    fun getCollectionItems(collectionId: UUID, pageable: Pageable): Page<CollectionItem>

    // ==================== Duplication ====================

    /**
     * Duplicates a collection to a new owner.
     * Requirement: 11.11
     *
     * @param collectionId The ID of the collection to duplicate
     * @param newOwnerId The ID of the new owner
     * @return The duplicated collection
     * @throws ResourceNotFoundException if collection not found
     * @throws UnauthorizedException if collection is not accessible
     */
    fun duplicateCollection(collectionId: UUID, newOwnerId: UUID): Collection

    /**
     * Checks if a collection contains a specific item.
     *
     * @param collectionId The collection ID
     * @param itemType The type of item
     * @param itemId The ID of the item
     * @return true if the collection contains the item
     */
    fun containsItem(collectionId: UUID, itemType: CollectionItemType, itemId: UUID): Boolean
}
