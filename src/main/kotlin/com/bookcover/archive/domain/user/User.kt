package com.bookcover.archive.domain.user

import com.bookcover.archive.domain.common.BaseEntity
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapKeyColumn
import jakarta.persistence.Table
import java.time.Instant

/**
 * Represents a user in the archive system.
 * Users can have different roles (publisher, author, designer, member, admin)
 * and earn titles based on their contributions.
 *
 * Requirements:
 * - 7.1: Users must select a role during registration
 * - 7.2: Website URL validation and storage
 * - 7.3: Portfolio URL validation and storage
 * - 7.4: SNS account information storage
 */
@Entity
@Table(name = "users")
class User(
    /** User's email address (unique identifier for login) */
    @Column(name = "email", nullable = false, unique = true)
    var email: String,

    /** User's display name shown in the UI */
    @Column(name = "display_name", nullable = false)
    var displayName: String,

    /** Hashed password for authentication */
    @Column(name = "password_hash", nullable = false)
    var passwordHash: String,

    /** User's role in the system - Requirement 7.1 */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    var role: UserRole,

    /** User's title/badge based on contributions - Requirement 7.5 */
    @Enumerated(EnumType.STRING)
    @Column(name = "title", nullable = false)
    var title: UserTitle = UserTitle.MEMBER,

    /** User's personal website URL - Requirement 7.2 */
    @Column(name = "website_url")
    var websiteUrl: String? = null,

    /** User's portfolio URL - Requirement 7.3 */
    @Column(name = "portfolio_url")
    var portfolioUrl: String? = null,

    /** User's SNS accounts (platform -> handle/url) - Requirement 7.4 */
    @ElementCollection
    @CollectionTable(
        name = "user_sns_accounts",
        joinColumns = [JoinColumn(name = "user_id")]
    )
    @MapKeyColumn(name = "platform")
    @Column(name = "account_handle")
    val snsAccounts: MutableMap<String, String> = mutableMapOf(),

    /** Number of books/content the user has contributed */
    @Column(name = "contribution_count", nullable = false)
    var contributionCount: Int = 0,

    /** Current account status */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: UserStatus = UserStatus.ACTIVE,

    /** Whether the user is restricted from reporting content */
    @Column(name = "report_restricted", nullable = false)
    var reportRestricted: Boolean = false,

    /** Timestamp until which the user is suspended (null if not suspended) */
    @Column(name = "suspended_until")
    var suspendedUntil: Instant? = null,

    /** Timestamp of the user's last login */
    @Column(name = "last_login_at")
    var lastLoginAt: Instant? = null
) : BaseEntity() {

    companion object {
        /** Minimum contributions required to earn Contributor title */
        const val CONTRIBUTOR_THRESHOLD = 10
    }

    /**
     * Increments the contribution count and updates title if threshold is reached.
     * Requirement 7.5: Auto-assign Contributor title at 10+ contributions
     */
    fun incrementContributionCount() {
        contributionCount++
        updateTitleIfEligible()
    }

    /**
     * Updates the user's title to CONTRIBUTOR if they have reached the threshold.
     */
    private fun updateTitleIfEligible() {
        if (contributionCount >= CONTRIBUTOR_THRESHOLD && title == UserTitle.MEMBER) {
            title = UserTitle.CONTRIBUTOR
        }
    }

    /**
     * Checks if the user is currently suspended.
     * @return true if the user is suspended and the suspension period has not ended
     */
    fun isSuspended(): Boolean {
        return status == UserStatus.SUSPENDED &&
            suspendedUntil != null &&
            Instant.now().isBefore(suspendedUntil)
    }

    /**
     * Checks if the user is active and can use the system.
     * @return true if the user is active
     */
    fun isActive(): Boolean = status == UserStatus.ACTIVE

    /**
     * Adds or updates an SNS account.
     * @param platform The SNS platform name (e.g., "twitter", "instagram")
     * @param handle The user's handle or URL on that platform
     */
    fun addSnsAccount(platform: String, handle: String) {
        snsAccounts[platform.lowercase()] = handle
    }

    /**
     * Removes an SNS account.
     * @param platform The SNS platform name to remove
     */
    fun removeSnsAccount(platform: String) {
        snsAccounts.remove(platform.lowercase())
    }

    /**
     * Records a login event.
     */
    fun recordLogin() {
        lastLoginAt = Instant.now()
    }
}
