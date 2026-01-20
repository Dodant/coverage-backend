package com.bookcover.archive.repository.book

import com.bookcover.archive.domain.book.OriginalBook
import com.bookcover.archive.domain.common.ContentStatus
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.Predicate
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

/**
 * Implementation of custom repository for advanced OriginalBook queries.
 *
 * Requirements:
 * - 12.1: ISBN duplicate detection
 * - 12.2: Similar book detection by title, author, year
 */
@Repository
class OriginalBookCustomRepositoryImpl : OriginalBookCustomRepository {

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    override fun findBySimilarTitle(title: String, pageable: Pageable): Page<OriginalBook> {
        val cb = entityManager.criteriaBuilder
        val query = cb.createQuery(OriginalBook::class.java)
        val root = query.from(OriginalBook::class.java)

        val normalizedTitle = title.lowercase().trim()

        // Search in both title and originalTitle fields
        val titlePredicate = cb.like(cb.lower(root.get("title")), "%$normalizedTitle%")
        val originalTitlePredicate = cb.like(cb.lower(root.get("originalTitle")), "%$normalizedTitle%")
        val statusPredicate = cb.equal(root.get<ContentStatus>("status"), ContentStatus.PUBLISHED)

        query.where(cb.and(cb.or(titlePredicate, originalTitlePredicate), statusPredicate))
        query.orderBy(cb.asc(root.get<String>("title")))

        val typedQuery = entityManager.createQuery(query)
        typedQuery.firstResult = pageable.offset.toInt()
        typedQuery.maxResults = pageable.pageSize

        val results = typedQuery.resultList

        // Count query
        val countQuery = cb.createQuery(Long::class.java)
        val countRoot = countQuery.from(OriginalBook::class.java)
        val countTitlePredicate = cb.like(cb.lower(countRoot.get("title")), "%$normalizedTitle%")
        val countOriginalTitlePredicate = cb.like(cb.lower(countRoot.get("originalTitle")), "%$normalizedTitle%")
        val countStatusPredicate = cb.equal(countRoot.get<ContentStatus>("status"), ContentStatus.PUBLISHED)
        countQuery.select(cb.count(countRoot))
        countQuery.where(cb.and(cb.or(countTitlePredicate, countOriginalTitlePredicate), countStatusPredicate))
        val total = entityManager.createQuery(countQuery).singleResult

        return PageImpl(results, pageable, total)
    }

    override fun findPotentialDuplicates(
        title: String,
        authorName: String,
        year: Int,
        pageable: Pageable
    ): Page<OriginalBook> {
        val cb = entityManager.criteriaBuilder
        val query = cb.createQuery(OriginalBook::class.java)
        val root = query.from(OriginalBook::class.java)
        val authorJoin = root.join<OriginalBook, Any>("author")

        val normalizedTitle = title.lowercase().trim()
        val normalizedAuthorName = authorName.lowercase().trim()

        val predicates = mutableListOf<Predicate>()

        // Title similarity (contains match)
        val titlePredicate = cb.or(
            cb.like(cb.lower(root.get("title")), "%$normalizedTitle%"),
            cb.like(cb.lower(root.get("originalTitle")), "%$normalizedTitle%")
        )
        predicates.add(titlePredicate)

        // Author name match
        val authorPredicate = cb.like(cb.lower(authorJoin.get("name")), "%$normalizedAuthorName%")
        predicates.add(authorPredicate)

        // Year range (within 2 years)
        val yearPredicate = cb.between(root.get("year"), year - 2, year + 2)
        predicates.add(yearPredicate)

        // Only published content
        val statusPredicate = cb.equal(root.get<ContentStatus>("status"), ContentStatus.PUBLISHED)
        predicates.add(statusPredicate)

        query.where(*predicates.toTypedArray())
        query.orderBy(cb.asc(root.get<String>("title")))

        val typedQuery = entityManager.createQuery(query)
        typedQuery.firstResult = pageable.offset.toInt()
        typedQuery.maxResults = pageable.pageSize

        val results = typedQuery.resultList

        // Count query
        val countQuery = cb.createQuery(Long::class.java)
        val countRoot = countQuery.from(OriginalBook::class.java)
        val countAuthorJoin = countRoot.join<OriginalBook, Any>("author")

        val countPredicates = mutableListOf<Predicate>()
        countPredicates.add(
            cb.or(
                cb.like(cb.lower(countRoot.get("title")), "%$normalizedTitle%"),
                cb.like(cb.lower(countRoot.get("originalTitle")), "%$normalizedTitle%")
            )
        )
        countPredicates.add(cb.like(cb.lower(countAuthorJoin.get("name")), "%$normalizedAuthorName%"))
        countPredicates.add(cb.between(countRoot.get("year"), year - 2, year + 2))
        countPredicates.add(cb.equal(countRoot.get<ContentStatus>("status"), ContentStatus.PUBLISHED))

        countQuery.select(cb.count(countRoot))
        countQuery.where(*countPredicates.toTypedArray())
        val total = entityManager.createQuery(countQuery).singleResult

        return PageImpl(results, pageable, total)
    }

    override fun searchBooks(
        title: String?,
        authorName: String?,
        publisherName: String?,
        genre: String?,
        countryCode: String?,
        yearFrom: Int?,
        yearTo: Int?,
        pageable: Pageable
    ): Page<OriginalBook> {
        val cb = entityManager.criteriaBuilder
        val query = cb.createQuery(OriginalBook::class.java)
        val root = query.from(OriginalBook::class.java)

        val predicates = mutableListOf<Predicate>()

        // Only published content
        predicates.add(cb.equal(root.get<ContentStatus>("status"), ContentStatus.PUBLISHED))

        // Title search
        title?.takeIf { it.isNotBlank() }?.let {
            val normalizedTitle = it.lowercase().trim()
            predicates.add(
                cb.or(
                    cb.like(cb.lower(root.get("title")), "%$normalizedTitle%"),
                    cb.like(cb.lower(root.get("originalTitle")), "%$normalizedTitle%")
                )
            )
        }

        // Author search
        authorName?.takeIf { it.isNotBlank() }?.let {
            val authorJoin = root.join<OriginalBook, Any>("author")
            predicates.add(cb.like(cb.lower(authorJoin.get("name")), "%${it.lowercase().trim()}%"))
        }

        // Publisher search
        publisherName?.takeIf { it.isNotBlank() }?.let {
            val publisherJoin = root.join<OriginalBook, Any>("publisher")
            predicates.add(cb.like(cb.lower(publisherJoin.get("name")), "%${it.lowercase().trim()}%"))
        }

        // Genre filter
        genre?.takeIf { it.isNotBlank() }?.let {
            predicates.add(cb.equal(cb.lower(root.get("genre")), it.lowercase().trim()))
        }

        // Country filter
        countryCode?.takeIf { it.isNotBlank() }?.let {
            predicates.add(cb.equal(root.get<String>("countryCode"), it.uppercase().trim()))
        }

        // Year range
        yearFrom?.let {
            predicates.add(cb.greaterThanOrEqualTo(root.get("year"), it))
        }
        yearTo?.let {
            predicates.add(cb.lessThanOrEqualTo(root.get("year"), it))
        }

        query.where(*predicates.toTypedArray())
        query.orderBy(cb.desc(root.get<Any>("createdAt")))

        val typedQuery = entityManager.createQuery(query)
        typedQuery.firstResult = pageable.offset.toInt()
        typedQuery.maxResults = pageable.pageSize

        val results = typedQuery.resultList

        // Count query
        val countQuery = cb.createQuery(Long::class.java)
        val countRoot = countQuery.from(OriginalBook::class.java)

        val countPredicates = mutableListOf<Predicate>()
        countPredicates.add(cb.equal(countRoot.get<ContentStatus>("status"), ContentStatus.PUBLISHED))

        title?.takeIf { it.isNotBlank() }?.let {
            val normalizedTitle = it.lowercase().trim()
            countPredicates.add(
                cb.or(
                    cb.like(cb.lower(countRoot.get("title")), "%$normalizedTitle%"),
                    cb.like(cb.lower(countRoot.get("originalTitle")), "%$normalizedTitle%")
                )
            )
        }

        authorName?.takeIf { it.isNotBlank() }?.let {
            val authorJoin = countRoot.join<OriginalBook, Any>("author")
            countPredicates.add(cb.like(cb.lower(authorJoin.get("name")), "%${it.lowercase().trim()}%"))
        }

        publisherName?.takeIf { it.isNotBlank() }?.let {
            val publisherJoin = countRoot.join<OriginalBook, Any>("publisher")
            countPredicates.add(cb.like(cb.lower(publisherJoin.get("name")), "%${it.lowercase().trim()}%"))
        }

        genre?.takeIf { it.isNotBlank() }?.let {
            countPredicates.add(cb.equal(cb.lower(countRoot.get("genre")), it.lowercase().trim()))
        }

        countryCode?.takeIf { it.isNotBlank() }?.let {
            countPredicates.add(cb.equal(countRoot.get<String>("countryCode"), it.uppercase().trim()))
        }

        yearFrom?.let {
            countPredicates.add(cb.greaterThanOrEqualTo(countRoot.get("year"), it))
        }
        yearTo?.let {
            countPredicates.add(cb.lessThanOrEqualTo(countRoot.get("year"), it))
        }

        countQuery.select(cb.count(countRoot))
        countQuery.where(*countPredicates.toTypedArray())
        val total = entityManager.createQuery(countQuery).singleResult

        return PageImpl(results, pageable, total)
    }
}
