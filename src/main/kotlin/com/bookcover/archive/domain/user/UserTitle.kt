package com.bookcover.archive.domain.user

/**
 * Represents the title/badge of a user based on their contributions.
 *
 * Requirements:
 * - 7.5: Members who register 10+ books receive "Contributor" title
 */
enum class UserTitle {
    /** Default title for regular members */
    MEMBER,

    /** Title awarded to members who have contributed 10+ books */
    CONTRIBUTOR
}
