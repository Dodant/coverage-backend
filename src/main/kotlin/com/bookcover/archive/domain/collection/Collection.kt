package com.bookcover.archive.domain.collection

import com.bookcover.archive.domain.common.BaseEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import java.util.UUID

/**
 * Represents a user's collection of book covers and redesigns.
 * Collections can be public, private, or shared via link.
 *
 * Requirements:
 * - 11.1: Collections have name, description, and visibility settings
 * - 11.9: Public collections have share links
 * - 11.10: Link-only sharing for read-only access
 */
@Entity
@Table(name = "collections")
class Collection(
    /** Name of the collection */
    @Column(name = "name", nullable = false)
    var name: String,

    /** Optional description of the collection */
    @Column(name = "description", length = 1000)
    var description: String? = null,

    /** Visibility setting of the collection */
    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false)
    var visibility: CollectionVisibility = CollectionVisibility.PRIVATE,

    /** Unique token for sharing the collection - Requirement 11.9 */
    @Column(name = "share_token", nullable = false, unique = true)
    val shareToken: String = UUID.randomUUID().toString(),

    /** UUID of the user who owns this collection */
    @Column(name = "owner_id", nullable = false)
    val ownerId: UUID,

    /** Items in this collection */
    @OneToMany(mappedBy = "collection", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    val items: MutableList<CollectionItem> = mutableListOf()
) : BaseEntity() {

    /**
     * Checks if the collection is private.
     * @return true if the collection is private
     */
    fun isPrivate(): Boolean = visibility == CollectionVisibility.PRIVATE

    /**
     * Checks if the collection is public.
     * @return true if the collection is public
     */
    fun isPublic(): Boolean = visibility == CollectionVisibility.PUBLIC

    /**
     * Checks if the collection is link-only.
     * @return true if the collection is link-only
     */
    fun isLinkOnly(): Boolean = visibility == CollectionVisibility.LINK_ONLY

    /**
     * Checks if a user can view this collection.
     * @param userId The UUID of the user trying to view
     * @param shareToken Optional share token provided
     * @return true if the user can view the collection
     */
    fun canBeViewedBy(userId: UUID?, shareToken: String? = null): Boolean {
        // Owner can always view
        if (userId == ownerId) return true

        return when (visibility) {
            CollectionVisibility.PUBLIC -> true
            CollectionVisibility.LINK_ONLY -> shareToken == this.shareToken
            CollectionVisibility.PRIVATE -> false
        }
    }

    /**
     * Adds an item to the collection.
     * @param itemType The type of item to add
     * @param itemId The UUID of the item
     * @return The created CollectionItem
     */
    fun addItem(itemType: CollectionItemType, itemId: UUID): CollectionItem {
        val nextOrder = items.maxOfOrNull { it.sortOrder }?.plus(1) ?: 0
        val item = CollectionItem(
            collection = this,
            itemType = itemType,
            itemId = itemId,
            sortOrder = nextOrder
        )
        items.add(item)
        return item
    }

    /**
     * Removes an item from the collection.
     * @param itemId The UUID of the item to remove
     * @return true if the item was removed
     */
    fun removeItem(itemId: UUID): Boolean {
        return items.removeIf { it.itemId == itemId }
    }

    /**
     * Removes an item from the collection by its CollectionItem ID.
     * @param collectionItemId The UUID of the CollectionItem to remove
     * @return true if the item was removed
     */
    fun removeItemById(collectionItemId: UUID): Boolean {
        return items.removeIf { it.id == collectionItemId }
    }

    /**
     * Gets the number of items in the collection.
     * @return The item count
     */
    fun itemCount(): Int = items.size

    /**
     * Checks if the collection contains a specific item.
     * @param itemType The type of item
     * @param itemId The UUID of the item
     * @return true if the collection contains the item
     */
    fun containsItem(itemType: CollectionItemType, itemId: UUID): Boolean {
        return items.any { it.itemType == itemType && it.itemId == itemId }
    }

    /**
     * Makes the collection public.
     */
    fun makePublic() {
        visibility = CollectionVisibility.PUBLIC
    }

    /**
     * Makes the collection private.
     */
    fun makePrivate() {
        visibility = CollectionVisibility.PRIVATE
    }

    /**
     * Makes the collection link-only.
     */
    fun makeLinkOnly() {
        visibility = CollectionVisibility.LINK_ONLY
    }
}
