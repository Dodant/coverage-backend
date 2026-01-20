package com.bookcover.archive.domain.user

/**
 * Represents the role of a user in the archive system.
 * Users select their role during registration.
 *
 * Requirements:
 * - 7.1: Users must select one of these roles during registration
 */
enum class UserRole {
    /** Publishing company representative */
    PUBLISHER,

    /** Book author */
    AUTHOR,

    /** Book cover designer */
    DESIGNER,

    /** General member */
    MEMBER,

    /** System administrator */
    ADMIN
}
