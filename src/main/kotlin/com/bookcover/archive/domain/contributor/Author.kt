package com.bookcover.archive.domain.contributor

import com.bookcover.archive.domain.common.BaseEntity
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import java.util.UUID

/**
 * Represents an author (writer) of books.
 * Supports multiple aliases for different name representations.
 */
@Entity
@Table(name = "authors")
class Author(
    /** Primary display name of the author */
    @Column(name = "name", nullable = false)
    var name: String,

    /** Alternative names/aliases for the author (pen names, translated names, etc.) */
    @ElementCollection
    @CollectionTable(name = "author_aliases", joinColumns = [JoinColumn(name = "author_id")])
    @Column(name = "alias")
    val aliases: MutableSet<String> = mutableSetOf(),

    /** Optional biography or description */
    @Column(name = "bio", length = 2000)
    var bio: String? = null,

    /** Optional website URL */
    @Column(name = "website_url")
    var websiteUrl: String? = null,

    /** Count of books associated with this author */
    @Column(name = "book_count", nullable = false)
    var bookCount: Int = 0
) : BaseEntity() {

    /**
     * Adds an alias to this author.
     * @param alias The alias to add
     * @return true if the alias was added, false if it already existed
     */
    fun addAlias(alias: String): Boolean = aliases.add(alias.trim())

    /**
     * Removes an alias from this author.
     * @param alias The alias to remove
     * @return true if the alias was removed, false if it didn't exist
     */
    fun removeAlias(alias: String): Boolean = aliases.remove(alias.trim())

    /**
     * Checks if this author has a specific name or alias.
     * @param nameOrAlias The name or alias to check
     * @return true if the author has this name or alias
     */
    fun hasNameOrAlias(nameOrAlias: String): Boolean {
        val normalized = nameOrAlias.trim().lowercase()
        return name.lowercase() == normalized || aliases.any { it.lowercase() == normalized }
    }
}
