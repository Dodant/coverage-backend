package com.bookcover.archive.repository.book

import com.bookcover.archive.domain.book.BookCover
import com.bookcover.archive.domain.book.CoverType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Repository for BookCover entity operations.
 *
 * Requirements:
 * - 1.4: Optional back cover support
 * - 14.8: Thumbnail and original image storage
 */
@Repository
interface BookCoverRepository : JpaRepository<BookCover, UUID> {

    /**
     * Finds all book covers by cover type.
     * @param coverType The type of cover (FRONT or BACK)
     * @return List of book covers of the specified type
     */
    fun findByCoverType(coverType: CoverType): List<BookCover>

    /**
     * Finds book covers by dominant color.
     * @param dominantColor The dominant color in HEX format
     * @return List of book covers with the specified dominant color
     */
    fun findByDominantColor(dominantColor: String): List<BookCover>
}
