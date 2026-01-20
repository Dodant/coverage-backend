package com.bookcover.archive.service

import com.bookcover.archive.domain.user.User
import java.util.UUID

/**
 * Service interface for tracking user contributions and managing titles.
 *
 * Requirements:
 * - 7.5: Auto-assign Contributor title when user registers 10+ books
 * - 7.6: Track contribution count for each user
 */
interface ContributionService {

    /**
     * Records a contribution for a user and updates their title if eligible.
     * Requirements: 7.5, 7.6
     *
     * @param userId The ID of the user who made the contribution
     * @return The updated user
     * @throws ResourceNotFoundException if user not found
     */
    fun recordContribution(userId: UUID): User

    /**
     * Gets the contribution count for a user.
     *
     * @param userId The user ID
     * @return The contribution count
     * @throws ResourceNotFoundException if user not found
     */
    fun getContributionCount(userId: UUID): Int

    /**
     * Checks if a user is eligible for the Contributor title.
     *
     * @param userId The user ID
     * @return true if the user has 10+ contributions
     * @throws ResourceNotFoundException if user not found
     */
    fun isEligibleForContributorTitle(userId: UUID): Boolean

    /**
     * Decrements the contribution count for a user (e.g., when content is deleted).
     *
     * @param userId The ID of the user
     * @return The updated user
     * @throws ResourceNotFoundException if user not found
     */
    fun decrementContribution(userId: UUID): User
}
