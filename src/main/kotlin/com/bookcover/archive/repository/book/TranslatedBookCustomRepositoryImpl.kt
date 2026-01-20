package com.bookcover.archive.repository.book

import com.bookcover.archive.domain.book.TranslatedBook
import com.bookcover.archive.domain.common.ContentStatus
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.Predicate
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

/**
 * Implementation of custom repository for advanced TranslatedBook queries.
 *
 * Requirements:
 * - 12.1: ISBN duplicate detection
 * - 12.2: Similar book detection
 */
@Repository
class TranslatedBookCustomRepositoryImpl : TranslatedBookCustomRepository {

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    override fun findBySimilarTitle(title: String, pageable: Pageable): Page<TranslatedBook> {
        val cb = entityManager.criteriaBuilder
        val query = cb.createQuery(TranslatedBook::class.java)
        val root = query.from(TranslatedBook::class.java)

        val normalizedTitle = title.lowercase().trim()

        val titlePredicate = cb.like(cb.lower(root.get("title")), "%$normalizedTitle%")
        val statusPredicate = cb.equal(root.get<ContentStatus>("status"), ContentStatus.PUBLISHED)

        query.where(cb.and(titlePredicate, statusPredicate))
        query.orderBy(cb.asc(root.get<String>("title")))

        val typedQuery = entityManager.createQuery(query)
        typedQuery.firstResult = pageable.offset.toInt()
        typedQuery.maxResults = pageable.pageSize

        val results = typedQuery.resultList

        // Count query
        val countQuery = cb.createQuery(Long::class.java)
        val countRoot = countQuery.from(TranslatedBook::class.java)
        countQuery.select(cb.count(countRoot))
        countQuery.where(
            cb.and(
                cb.like(cb.lower(countRoot.get("title")), "%$normalizedTitle%"),
                cb.equal(countRoot.get<ContentStatus>("status"), ContentStatus.PUBLISHED)
            )
        )
        val total = entityManager.createQuery(countQuery).singleResult

        return PageImpl(results, pageable, total)
    }

    override fun findPotentialDuplicates(
        title: String,
        translatorName: String,
        year: Int,
        pageable: Pageable
    ): Page<TranslatedBook> {
        val cb = entityManager.criteriaBuilder
        val query = cb.createQuery(TranslatedBook::class.java)
        val root = query.from(TranslatedBook::class.java)
        val translatorJoin = root.join<TranslatedBook, Any>("translator")

        val normalizedTitle = title.lowercase().trim()
        val normalizedTranslatorName = translatorName.lowercase().trim()

        val predicates = mutableListOf<Predicate>()

        // Title similarity
        predicates.add(cb.like(cb.lower(root.get("title")), "%$normalizedTitle%"))

        // Translator name match
        predicates.add(cb.like(cb.lower(translatorJoin.get("name")), "%$normalizedTranslatorName%"))

        // Year range (within 2 years)
        predicates.add(cb.between(root.get("year"), year - 2, year + 2))

        // Only published content
        predicates.add(cb.equal(root.get<ContentStatus>("status"), ContentStatus.PUBLISHED))

        query.where(*predicates.toTypedArray())
        query.orderBy(cb.asc(root.get<String>("title")))

        val typedQuery = entityManager.createQuery(query)
        typedQuery.firstResult = pageable.offset.toInt()
        typedQuery.maxResults = pageable.pageSize

        val results = typedQuery.resultList

        // Count query
        val countQuery = cb.createQuery(Long::class.java)
        val countRoot = countQuery.from(TranslatedBook::class.java)
        val countTranslatorJoin = countRoot.join<TranslatedBook, Any>("translator")

        val countPredicates = mutableListOf<Predicate>()
        countPredicates.add(cb.like(cb.lower(countRoot.get("title")), "%$normalizedTitle%"))
        countPredicates.add(cb.like(cb.lower(countTranslatorJoin.get("name")), "%$normalizedTranslatorName%"))
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
        translatorName: String?,
        publisherName: String?,
        countryCode: String?,
        languageCode: String?,
        yearFrom: Int?,
        yearTo: Int?,
        pageable: Pageable
    ): Page<TranslatedBook> {
        val cb = entityManager.criteriaBuilder
        val query = cb.createQuery(TranslatedBook::class.java)
        val root = query.from(TranslatedBook::class.java)

        val predicates = mutableListOf<Predicate>()

        // Only published content
        predicates.add(cb.equal(root.get<ContentStatus>("status"), ContentStatus.PUBLISHED))

        // Title search
        title?.takeIf { it.isNotBlank() }?.let {
            predicates.add(cb.like(cb.lower(root.get("title")), "%${it.lowercase().trim()}%"))
        }

        // Author search
        authorName?.takeIf { it.isNotBlank() }?.let {
            val authorJoin = root.join<TranslatedBook, Any>("author")
            predicates.add(cb.like(cb.lower(authorJoin.get("name")), "%${it.lowercase().trim()}%"))
        }

        // Translator search
        translatorName?.takeIf { it.isNotBlank() }?.let {
            val translatorJoin = root.join<TranslatedBook, Any>("translator")
            predicates.add(cb.like(cb.lower(translatorJoin.get("name")), "%${it.lowercase().trim()}%"))
        }

        // Publisher search
        publisherName?.takeIf { it.isNotBlank() }?.let {
            val publisherJoin = root.join<TranslatedBook, Any>("publisher")
            predicates.add(cb.like(cb.lower(publisherJoin.get("name")), "%${it.lowercase().trim()}%"))
        }

        // Country filter
        countryCode?.takeIf { it.isNotBlank() }?.let {
            predicates.add(cb.equal(root.get<String>("countryCode"), it.uppercase().trim()))
        }

        // Language filter
        languageCode?.takeIf { it.isNotBlank() }?.let {
            predicates.add(cb.equal(root.get<String>("languageCode"), it.lowercase().trim()))
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
        val countRoot = countQuery.from(TranslatedBook::class.java)

        val countPredicates = mutableListOf<Predicate>()
        countPredicates.add(cb.equal(countRoot.get<ContentStatus>("status"), ContentStatus.PUBLISHED))

        title?.takeIf { it.isNotBlank() }?.let {
            countPredicates.add(cb.like(cb.lower(countRoot.get("title")), "%${it.lowercase().trim()}%"))
        }

        authorName?.takeIf { it.isNotBlank() }?.let {
            val authorJoin = countRoot.join<TranslatedBook, Any>("author")
            countPredicates.add(cb.like(cb.lower(authorJoin.get("name")), "%${it.lowercase().trim()}%"))
        }

        translatorName?.takeIf { it.isNotBlank() }?.let {
            val translatorJoin = countRoot.join<TranslatedBook, Any>("translator")
            countPredicates.add(cb.like(cb.lower(translatorJoin.get("name")), "%${it.lowercase().trim()}%"))
        }

        publisherName?.takeIf { it.isNotBlank() }?.let {
            val publisherJoin = countRoot.join<TranslatedBook, Any>("publisher")
            countPredicates.add(cb.like(cb.lower(publisherJoin.get("name")), "%${it.lowercase().trim()}%"))
        }

        countryCode?.takeIf { it.isNotBlank() }?.let {
            countPredicates.add(cb.equal(countRoot.get<String>("countryCode"), it.uppercase().trim()))
        }

        languageCode?.takeIf { it.isNotBlank() }?.let {
            countPredicates.add(cb.equal(countRoot.get<String>("languageCode"), it.lowercase().trim()))
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
