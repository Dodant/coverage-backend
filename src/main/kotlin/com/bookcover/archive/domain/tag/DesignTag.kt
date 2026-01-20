package com.bookcover.archive.domain.tag

import com.bookcover.archive.domain.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table

/**
 * Represents a design attribute tag that can be applied to book covers.
 * Tags help users filter and browse covers by design characteristics.
 *
 * Requirements:
 * - 20.1: Support design attribute tags (typography, illustration, photo, minimal, retro, etc.)
 * - 20.4: Admin can manage tag dictionary and approval policy
 */
@Entity
@Table(name = "design_tags")
class DesignTag(
    /** The tag name (e.g., "minimal", "retro", "typography-focused") */
    @Column(name = "name", nullable = false, unique = true)
    var name: String,

    /** The category this tag belongs to */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    val category: TagCategory,

    /** Number of times this tag has been used on covers */
    @Column(name = "usage_count", nullable = false)
    var usageCount: Int = 0,

    /** Whether this tag has been approved by admin for public use */
    @Column(name = "approved", nullable = false)
    var approved: Boolean = true
) : BaseEntity() {

    /**
     * Increments the usage count when this tag is applied to a cover.
     */
    fun incrementUsageCount() {
        usageCount++
    }

    /**
     * Decrements the usage count when this tag is removed from a cover.
     * Ensures count doesn't go below zero.
     */
    fun decrementUsageCount() {
        if (usageCount > 0) {
            usageCount--
        }
    }

    /**
     * Approves this tag for public use.
     */
    fun approve() {
        approved = true
    }

    /**
     * Rejects/unapproves this tag, hiding it from public use.
     */
    fun reject() {
        approved = false
    }

    /**
     * Checks if this tag is available for public use.
     * @return true if the tag is approved
     */
    fun isAvailable(): Boolean = approved

    /**
     * Updates the tag name.
     * @param newName The new name for the tag
     * @throws IllegalArgumentException if the name is blank
     */
    fun updateName(newName: String) {
        require(newName.isNotBlank()) { "Tag name cannot be blank" }
        name = newName.trim().lowercase()
    }
}
