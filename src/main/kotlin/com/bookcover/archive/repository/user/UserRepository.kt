package com.bookcover.archive.repository.user

import com.bookcover.archive.domain.user.User
import com.bookcover.archive.domain.user.UserRole
import com.bookcover.archive.domain.user.UserStatus
import com.bookcover.archive.domain.user.UserTitle
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Repository for User entity operations.
 *
 * Requirements:
 * - 7.1: User role selection during registration
 * - 7.5: Contributor title auto-assignment
 * - 7.6: Contribution count tracking
 */
@Repository
interface UserRepository : JpaRepository<User, UUID> {

    /**
     * Finds a user by email address.
     * @param email The email address
     * @return The user if found
     */
    fun findByEmail(email: String): User?

    /**
     * Checks if a user with the given email exists.
     * @param email The email address
     * @return true if a user with this email exists
     */
    fun existsByEmail(email: String): Boolean

    /**
     * Finds a user by display name.
     * @param displayName The display name
     * @return The user if found
     */
    fun findByDisplayName(displayName: String): User?

    /**
     * Finds all users by role.
     * @param role The user role
     * @param pageable Pagination information
     * @return Page of users with the role
     */
    fun findByRole(role: UserRole, pageable: Pageable): Page<User>

    /**
     * Finds all users by title.
     * @param title The user title
     * @param pageable Pagination information
     * @return Page of users with the title
     */
    fun findByTitle(title: UserTitle, pageable: Pageable): Page<User>

    /**
     * Finds all users by status.
     * @param status The user status
     * @param pageable Pagination information
     * @return Page of users with the status
     */
    fun findByStatus(status: UserStatus, pageable: Pageable): Page<User>

    /**
     * Finds all contributors (users with CONTRIBUTOR title).
     * @param pageable Pagination information
     * @return Page of contributors
     */
    fun findByTitleAndStatus(title: UserTitle, status: UserStatus, pageable: Pageable): Page<User>

    /**
     * Counts users by role.
     * @param role The user role
     * @return Count of users with the role
     */
    fun countByRole(role: UserRole): Long

    /**
     * Counts users by status.
     * @param status The user status
     * @return Count of users with the status
     */
    fun countByStatus(status: UserStatus): Long

    /**
     * Counts users by title.
     * @param title The user title
     * @return Count of users with the title
     */
    fun countByTitle(title: UserTitle): Long

    /**
     * Finds users with contribution count greater than or equal to threshold.
     * @param contributionCount The minimum contribution count
     * @param pageable Pagination information
     * @return Page of users meeting the threshold
     */
    fun findByContributionCountGreaterThanEqual(contributionCount: Int, pageable: Pageable): Page<User>

    /**
     * Finds users who are restricted from reporting.
     * @param pageable Pagination information
     * @return Page of restricted users
     */
    fun findByReportRestrictedTrue(pageable: Pageable): Page<User>
}
