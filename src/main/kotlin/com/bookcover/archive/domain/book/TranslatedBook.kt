package com.bookcover.archive.domain.book

import com.bookcover.archive.domain.common.ContentStatus
import com.bookcover.archive.domain.contributor.Author
import com.bookcover.archive.domain.contributor.Publisher
import com.bookcover.archive.domain.contributor.Translator
import com.bookcover.archive.domain.media.Media
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.util.UUID

/**
 * Represents a translated book (번역서) in the archive system.
 * Extends Media to inherit common media properties.
 * Must be linked to an OriginalBook.
 *
 * Requirements:
 * - 2.1: Translated book must be linked to an original book
 * - 2.2: Required fields - title, country, language, year, author, translator, publisher, front cover
 * - 2.5: Edition information linked to translated book
 */
@Entity
@Table(name = "translated_books")
@DiscriminatorValue("BOOK_TRANSLATED")
class TranslatedBook(
    /** Display title of the translated book (in the target language) */
    title: String,

    /** ISO 3166-1 country code of the translation */
    countryCode: String,

    /** ISO 639-1 language code of the translation */
    languageCode: String,

    /** Publication year of the translation */
    year: Int,

    /** UUID of the user who created this entry */
    createdById: UUID,

    /** ISBN (International Standard Book Number) - optional but unique if provided */
    @Column(name = "isbn", unique = true)
    var isbn: String? = null,

    /** The original book this translation is based on - Requirement 2.1 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "original_book_id", nullable = false)
    var originalBook: OriginalBook,

    /** Author of the original work (same as original book's author) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    var author: Author,

    /** Translator of this edition - Requirement 2.2 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "translator_id", nullable = false)
    var translator: Translator,

    /** Publisher of the translated edition */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publisher_id", nullable = false)
    var publisher: Publisher,

    /** Edition information (e.g., "1st Edition", "Revised Edition") - Requirement 2.5 */
    @Column(name = "edition")
    var edition: String? = null,

    /** Front cover image (required) - Requirement 2.2 */
    @OneToOne(cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "front_cover_id", nullable = false)
    var frontCover: BookCover,

    /** Back cover image (optional) */
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
    /**
     * Gets the original title from the linked original book.
     * @return The original title of the work
     */
    fun getOriginalTitle(): String = originalBook.originalTitle

    /**
     * Gets the original language from the linked original book.
     * @return The original language code
     */
    fun getOriginalLanguage(): String = originalBook.languageCode

    /**
     * Gets the genre from the linked original book.
     * @return The genre of the work
     */
    fun getGenre(): String = originalBook.genre
}
