package com.bookcover.archive.domain.book

import com.bookcover.archive.domain.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table

/**
 * Represents a book cover image with its metadata.
 * Stores both original and thumbnail URLs for optimized loading.
 *
 * Requirements:
 * - 1.4: Optional back cover support
 * - 14.8: Thumbnail and original image storage for performance optimization
 */
@Entity
@Table(name = "book_covers")
class BookCover(
    /** URL to the full-resolution cover image */
    @Column(name = "image_url", nullable = false)
    var imageUrl: String,

    /** URL to the thumbnail version of the cover image */
    @Column(name = "thumbnail_url", nullable = false)
    var thumbnailUrl: String,

    /** Original filename of the uploaded image */
    @Column(name = "original_file_name", nullable = false)
    var originalFileName: String,

    /** Image width in pixels */
    @Column(name = "width", nullable = false)
    var width: Int,

    /** Image height in pixels */
    @Column(name = "height", nullable = false)
    var height: Int,

    /** File size in bytes */
    @Column(name = "file_size", nullable = false)
    var fileSize: Long,

    /** Type of cover (front or back) */
    @Enumerated(EnumType.STRING)
    @Column(name = "cover_type", nullable = false)
    var coverType: CoverType,

    /** Source of the image (scan, photo, link, etc.) */
    @Enumerated(EnumType.STRING)
    @Column(name = "image_source")
    var imageSource: ImageSource? = null,

    /** Source URL if the image was obtained from a link */
    @Column(name = "source_url")
    var sourceUrl: String? = null,

    /** Dominant color of the cover in HEX format (e.g., "#FF5733") */
    @Column(name = "dominant_color", length = 7)
    var dominantColor: String? = null,

    /** Layout style of the cover design */
    @Enumerated(EnumType.STRING)
    @Column(name = "layout")
    var layout: CoverLayout? = null,

    /** Number of likes this cover has received */
    @Column(name = "like_count", nullable = false)
    var likeCount: Int = 0,

    /** Number of comments on this cover */
    @Column(name = "comment_count", nullable = false)
    var commentCount: Int = 0
) : BaseEntity() {

    /**
     * Increments the like count by 1.
     */
    fun incrementLikeCount() {
        likeCount++
    }

    /**
     * Decrements the like count by 1, ensuring it doesn't go below 0.
     */
    fun decrementLikeCount() {
        if (likeCount > 0) {
            likeCount--
        }
    }

    /**
     * Increments the comment count by 1.
     */
    fun incrementCommentCount() {
        commentCount++
    }

    /**
     * Decrements the comment count by 1, ensuring it doesn't go below 0.
     */
    fun decrementCommentCount() {
        if (commentCount > 0) {
            commentCount--
        }
    }
}

/**
 * Type of book cover.
 */
enum class CoverType {
    /** Front cover of the book */
    FRONT,
    /** Back cover of the book */
    BACK
}

/**
 * Source of the cover image.
 */
enum class ImageSource {
    /** Image was scanned from a physical book */
    SCAN,
    /** Image was photographed */
    PHOTO,
    /** Image was obtained from a web link */
    LINK,
    /** Other source */
    OTHER
}

/**
 * Layout style of the cover design.
 */
enum class CoverLayout {
    /** Center-aligned design */
    CENTER,
    /** Left-aligned design */
    LEFT,
    /** Right-aligned design */
    RIGHT,
    /** Full bleed design (extends to edges) */
    FULL_BLEED,
    /** Minimal design */
    MINIMAL
}
