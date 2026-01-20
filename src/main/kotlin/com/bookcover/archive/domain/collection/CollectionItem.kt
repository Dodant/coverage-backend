package com.bookcover.archive.domain.collection

import com.bookcover.archive.domain.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

/**
 * Represents an item within a collection.
 * Uses polymorphic design to support different content types (covers, redesigns).
 * Each item can only appear once in a collection (enforced by unique constraint).
 */
@Entity
@Table(
    name = "collection_items",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_collection_items_collection_item",
            columnNames = ["collection_id", "item_type", "item_id"]
        )
    ]
)
class CollectionItem(
    /** The collection this item belongs to */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "collection_id", nullable = false)
    val collection: Collection,

    /** Type of the item */
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false)
    val itemType: CollectionItemType,

    /** UUID of the item (cover or redesign) */
    @Column(name = "item_id", nullable = false)
    val itemId: UUID,

    /** Order/position of the item within the collection */
    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0
) : BaseEntity() {

    /**
     * Checks if this item is an original book cover.
     * @return true if the item is an original cover
     */
    fun isOriginalCover(): Boolean = itemType == CollectionItemType.ORIGINAL_COVER

    /**
     * Checks if this item is a translated book cover.
     * @return true if the item is a translated cover
     */
    fun isTranslatedCover(): Boolean = itemType == CollectionItemType.TRANSLATED_COVER

    /**
     * Checks if this item is a redesign.
     * @return true if the item is a redesign
     */
    fun isRedesign(): Boolean = itemType == CollectionItemType.REDESIGN
}
