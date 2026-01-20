package com.bookcover.archive.repository.collection

import com.bookcover.archive.domain.collection.CollectionItem
import com.bookcover.archive.domain.collection.CollectionItemType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Repository for CollectionItem entity operations.
 *
 * Requirements:
 * - 11.2: Add book covers to collections
 * - 11.3: Support original covers, translated covers, and redesigns
 * - 11.7: Remove items from collections
 */
@Repository
interface CollectionItemRepository : JpaRepository<CollectionItem, UUID> {

    /**
     * Finds all items in a collection.
     * @param collectionId The collection's UUID
     * @param pageable Pagination information
     * @return Page of collection items
     */
    fun findByCollectionId(collectionId: UUID, pageable: Pageable): Page<CollectionItem>

    /**
     * Finds all items in a collection (unpaged).
     * @param collectionId The collection's UUID
     * @return List of collection items
     */
    fun findByCollectionId(collectionId: UUID): List<CollectionItem>

    /**
     * Finds items in a collection by type.
     * @param collectionId The collection's UUID
     * @param itemType The type of item
     * @param pageable Pagination information
     * @return Page of collection items
     */
    fun findByCollectionIdAndItemType(
        collectionId: UUID,
        itemType: CollectionItemType,
        pageable: Pageable
    ): Page<CollectionItem>

    /**
     * Checks if an item exists in a collection.
     * @param collectionId The collection's UUID
     * @param itemType The type of item
     * @param itemId The item's UUID
     * @return true if the item exists in the collection
     */
    fun existsByCollectionIdAndItemTypeAndItemId(
        collectionId: UUID,
        itemType: CollectionItemType,
        itemId: UUID
    ): Boolean

    /**
     * Finds a specific item in a collection.
     * @param collectionId The collection's UUID
     * @param itemType The type of item
     * @param itemId The item's UUID
     * @return The collection item if found
     */
    fun findByCollectionIdAndItemTypeAndItemId(
        collectionId: UUID,
        itemType: CollectionItemType,
        itemId: UUID
    ): CollectionItem?

    /**
     * Deletes an item from a collection.
     * Requirement 11.7: Remove items from collections
     * @param collectionId The collection's UUID
     * @param itemType The type of item
     * @param itemId The item's UUID
     */
    fun deleteByCollectionIdAndItemTypeAndItemId(
        collectionId: UUID,
        itemType: CollectionItemType,
        itemId: UUID
    )

    /**
     * Deletes all items from a collection.
     * @param collectionId The collection's UUID
     */
    fun deleteByCollectionId(collectionId: UUID)

    /**
     * Counts items in a collection.
     * @param collectionId The collection's UUID
     * @return Count of items
     */
    fun countByCollectionId(collectionId: UUID): Long

    /**
     * Counts items of a specific type in a collection.
     * @param collectionId The collection's UUID
     * @param itemType The type of item
     * @return Count of items
     */
    fun countByCollectionIdAndItemType(collectionId: UUID, itemType: CollectionItemType): Long

    /**
     * Finds all collections containing a specific item.
     * @param itemType The type of item
     * @param itemId The item's UUID
     * @return List of collection items
     */
    fun findByItemTypeAndItemId(itemType: CollectionItemType, itemId: UUID): List<CollectionItem>

    /**
     * Counts how many collections contain a specific item.
     * @param itemType The type of item
     * @param itemId The item's UUID
     * @return Count of collections
     */
    fun countByItemTypeAndItemId(itemType: CollectionItemType, itemId: UUID): Long
}
