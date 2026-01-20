package com.bookcover.archive.domain.book

import com.bookcover.archive.domain.common.ContentStatus
import com.bookcover.archive.domain.contributor.Author
import com.bookcover.archive.domain.contributor.Designer
import com.bookcover.archive.domain.contributor.Publisher
import com.bookcover.archive.domain.media.Media
import com.bookcover.archive.domain.work.Work
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.util.UUID

/**
 * Represents an original book (원서) in the archive system.
 * Extends Media to inherit common media properties.
 *
 * Requirements:
 * - 1.1: Required fields - title, original title, country, language, year, author, publisher, genre, front cover
 * - 1.5: Unique identifier generation and database storage
 * - 1.6: Designer information linked to original book
 * - 1.7: Edition information linked to original book
 */
@Entity
@Table(name = "original_books")
@DiscriminatorValue("BOOK_ORIGINAL")
class OriginalBook(
    /** Display title of the book */
    title: String,

    /** ISO 3166-1 country code */
    countryCode: String,

    /** ISO 639-1 language code */
    languageCode: String,

    /** Publication year */
    year: Int,

    /** UUID of the user who created this entry */
    createdById: UUID,

    /** Original title of the book (in its original language) */
    @Column(name = "original_title", nullable = false)
    var originalTitle: String,

    /** Genre of the book (e.g., fiction, non-fiction, poetry) */
    @Column(name = "genre", nullable = false)
    var genre: String,

    /** ISBN (International Standard Book Number) - optional but unique if provided */
    @Column(name = "isbn", unique = true)
    var isbn: String? = null,

    /** Author of the book */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    var author: Author,

    /** Publisher of the book */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publisher_id", nullable = false)
    var publisher: Publisher,

    /** Designer of the book cover (optional) - Requirement 1.6 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "designer_id")
    var designer: Designer? = null,

    /** Edition information (e.g., "1st Edition", "Special Edition") - Requirement 1.7 */
    @Column(name = "edition")
    var edition: String? = null,

    /** Work entity for grouping same works across different editions */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_id")
    var work: Work? = null,

    /** Front cover image (required) - Requirement 1.1 */
    @OneToOne(cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "front_cover_id", nullable = false)
    var frontCover: BookCover,

    /** Back cover image (optional) - Requirement 1.4 */
    @OneToOne(cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "back_cover_id")
    var backCover: BookCover? = null,

    /** Content status */
    status: ContentStatus = ContentStatus.PUBLISHED
) : Media(
    title = title,
    countryCode = countryCode,
    languageCode = languageCode,
    year = year,
    status = status,
    createdById = createdById
) {
    /** List of translations of this original book */
    @OneToMany(mappedBy = "originalBook", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val translations: MutableList<TranslatedBook> = mutableListOf()

    /**
     * Adds a translation to this original book.
     * @param translation The translated book to add
     */
    fun addTranslation(translation: TranslatedBook) {
        translations.add(translation)
    }

    /**
     * Removes a translation from this original book.
     * @param translation The translated book to remove
     * @return true if the translation was removed
     */
    fun removeTranslation(translation: TranslatedBook): Boolean {
        return translations.remove(translation)
    }

    /**
     * Gets the count of translations for this original book.
     * @return The number of translations
     */
    fun getTranslationCount(): Int = translations.size
}
