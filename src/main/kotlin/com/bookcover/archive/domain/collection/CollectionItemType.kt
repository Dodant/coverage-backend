package com.bookcover.archive.domain.collection

/**
 * Represents the type of item that can be added to a collection.
 * Supports polymorphic collection items across different content types.
 */
enum class CollectionItemType {
    /** Original book cover */
    ORIGINAL_COVER,

    /** Translated book cover */
    TRANSLATED_COVER,

    /** User redesign */
    REDESIGN
}
