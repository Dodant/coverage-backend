package com.bookcover.archive.domain.user

/**
 * Represents the account status of a user.
 */
enum class UserStatus {
    /** User account is active and can use all features */
    ACTIVE,

    /** User account is temporarily suspended */
    SUSPENDED,

    /** User account has been deleted */
    DELETED
}
