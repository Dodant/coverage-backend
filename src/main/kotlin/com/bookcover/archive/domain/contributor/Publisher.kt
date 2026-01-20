package com.bookcover.archive.domain.contributor

import com.bookcover.archive.domain.common.BaseEntity
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table

/**
 * Represents a publishing company.
 * Supports multiple aliases for different name representations.
 */
@Entity
@Table(name = "publishers")
class Publisher(
    /** Primary display name of the publisher */
    @Column(name = "name", nullable = false)
    var name: String,

    /** Alternative names/aliases for the publisher (translated names, abbreviations, etc.) */
    @ElementCollection
    @CollectionTable(name = "publisher_aliases", joinColumns = [JoinColumn(name = "publisher_id")])
    @Column(name = "alias")
    val aliases: MutableSet<String> = mutableSetOf(),

    /** Optional description of the publisher */
    @Column(name = "description", length = 2000)
    var description: String? = null,

    /** Optional website URL */
    @Column(name = "website_url")
    var websiteUrl: String? = null,

    /** ISO 3166-1 country code where the publisher is based */
    @Column(name = "country_code", length = 2)
    var countryCode: String? = null,

    /** Count of books published by this publisher in the archive */
    @Column(name = "book_count", nullable = false)
    var bookCount: Int = 0
) : BaseEntity() {

    /**
     * Adds an alias to this publisher.
     * @param alias The alias to add
     * @return true if the alias was added, false if it already existed
     */
    fun addAlias(alias: String): Boolean = aliases.add(alias.trim())

    /**
     * Removes an alias from this publisher.
     * @param alias The alias to remove
     * @return true if the alias was removed, false if it didn't exist
     */
    fun removeAlias(alias: String): Boolean = aliases.remove(alias.trim())

    /**
     * Checks if this publisher has a specific name or alias.
     * @param nameOrAlias The name or alias to check
     * @return true if the publisher has this name or alias
     */
    fun hasNameOrAlias(nameOrAlias: String): Boolean {
        val normalized = nameOrAlias.trim().lowercase()
        return name.lowercase() == normalized || aliases.any { it.lowercase() == normalized }
    }
}
