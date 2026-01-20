package com.bookcover.archive.domain.media

import com.bookcover.archive.domain.common.ContentStatus
import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.DiscriminatorType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * Abstract base class for all media types in the archive system.
 * Designed for extensibility to support book covers, movie posters, and other media types.
 *
 * Uses JOINED inheritance strategy for optimal query performance and data normalization.
 * Each subclass will have its own table with a foreign key to this base table.
 */
@Entity
@Table(name = "media")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "media_type", discriminatorType = DiscriminatorType.STRING)
abstract class Media(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    open val id: UUID = UUID.randomUUID(),

    /** Display title of the media */
    @Column(name = "title", nullable = false)
    open var title: String,

    /** ISO 3166-1 country code (e.g., "KR", "US", "JP") */
    @Column(name = "country_code", nullable = false, length = 2)
    open val countryCode: String,

    /** ISO 639-1 language code (e.g., "ko", "en", "ja") */
    @Column(name = "language_code", nullable = false, length = 2)
    open val languageCode: String,

    /** Publication year (4-digit format) */
    @Column(name = "year", nullable = false)
    open val year: Int,

    /** Current publication status of the media */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    open var status: ContentStatus = ContentStatus.PUBLISHED,

    /** UUID of the user who created this entry */
    @Column(name = "created_by_id", nullable = false)
    open val createdById: UUID,

    @Column(name = "created_at", nullable = false, updatable = false)
    open var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    open var updatedAt: Instant = Instant.now()
) {
    @PrePersist
    fun prePersist() {
        val now = Instant.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = Instant.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Media) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
