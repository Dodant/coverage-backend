package com.bookcover.archive.domain.work

import com.bookcover.archive.domain.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

/**
 * Represents a literary work that can have multiple editions across different publishers.
 * Used to group different editions (original books, translations) of the same work together.
 *
 * For example, "1984" by George Orwell is a single Work, but it may have:
 * - Multiple original editions from different publishers
 * - Translations in various languages
 * - Special editions, anniversary editions, etc.
 *
 * This enables comparison of book covers across different editions of the same work.
 */
@Entity
@Table(name = "works")
class Work(
    /** The canonical/original title of the work */
    @Column(name = "original_title", nullable = false)
    var originalTitle: String,

    /** Optional description or summary of the work */
    @Column(name = "description", length = 4000)
    var description: String? = null,

    /** The original language of the work (ISO 639-1 code) */
    @Column(name = "original_language_code", length = 2)
    var originalLanguageCode: String? = null,

    /** The year the work was first published */
    @Column(name = "first_published_year")
    var firstPublishedYear: Int? = null,

    /** Count of editions (original books + translations) linked to this work */
    @Column(name = "edition_count", nullable = false)
    var editionCount: Int = 0
) : BaseEntity() {

    /**
     * Increments the edition count when a new edition is linked to this work.
     */
    fun incrementEditionCount() {
        editionCount++
    }

    /**
     * Decrements the edition count when an edition is unlinked from this work.
     */
    fun decrementEditionCount() {
        if (editionCount > 0) {
            editionCount--
        }
    }
}
