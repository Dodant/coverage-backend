package com.bookcover.archive.repository;

import com.bookcover.archive.domain.media.Media;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * 책 검색을 위한 커스텀 Repository 구현체.
 * 중복 검사 및 유사 항목 검색 쿼리를 구현합니다.
 *
 * Requirements:
 * - 12.1: 동일한 ISBN이 이미 존재하면 중복 경고를 표시하고 기존 엔트리 정보를 보여준다
 * - 12.2: 제목, 저자, 연도가 유사한 책이 존재하면 유사 항목 목록을 표시한다
 */
@Repository
public class BookSearchRepositoryImpl implements BookSearchRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Media> findDuplicatesByIsbn(String isbn) {
        if (isbn == null || isbn.isBlank()) {
            return List.of();
        }

        List<Media> results = new ArrayList<>();

        // 원서에서 ISBN 검색
        String originalQuery = "SELECT o FROM OriginalBook o WHERE o.isbn = :isbn";
        TypedQuery<Media> originalBookQuery = entityManager.createQuery(originalQuery, Media.class);
        originalBookQuery.setParameter("isbn", isbn);
        results.addAll(originalBookQuery.getResultList());

        // 번역서에서 ISBN 검색
        String translatedQuery = "SELECT t FROM TranslatedBook t WHERE t.isbn = :isbn";
        TypedQuery<Media> translatedBookQuery = entityManager.createQuery(translatedQuery, Media.class);
        translatedBookQuery.setParameter("isbn", isbn);
        results.addAll(translatedBookQuery.getResultList());

        return results;
    }


    @Override
    public List<Media> findSimilarBooks(String title, String authorName, int year) {
        List<Media> results = new ArrayList<>();

        // 원서에서 유사 항목 검색
        String originalQuery = """
            SELECT o FROM OriginalBook o
            LEFT JOIN o.author a
            WHERE (LOWER(o.title) LIKE LOWER(CONCAT('%', :title, '%'))
               OR LOWER(o.originalTitle) LIKE LOWER(CONCAT('%', :title, '%')))
              AND (a.name LIKE CONCAT('%', :authorName, '%') OR :authorName IS NULL OR :authorName = '')
              AND (o.year BETWEEN :yearStart AND :yearEnd)
            """;
        TypedQuery<Media> originalBookQuery = entityManager.createQuery(originalQuery, Media.class);
        originalBookQuery.setParameter("title", title);
        originalBookQuery.setParameter("authorName", authorName);
        originalBookQuery.setParameter("yearStart", year - 2);
        originalBookQuery.setParameter("yearEnd", year + 2);
        originalBookQuery.setMaxResults(20);
        results.addAll(originalBookQuery.getResultList());

        // 번역서에서 유사 항목 검색
        String translatedQuery = """
            SELECT t FROM TranslatedBook t
            LEFT JOIN t.author a
            WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :title, '%'))
              AND (a.name LIKE CONCAT('%', :authorName, '%') OR :authorName IS NULL OR :authorName = '')
              AND (t.year BETWEEN :yearStart AND :yearEnd)
            """;
        TypedQuery<Media> translatedBookQuery = entityManager.createQuery(translatedQuery, Media.class);
        translatedBookQuery.setParameter("title", title);
        translatedBookQuery.setParameter("authorName", authorName);
        translatedBookQuery.setParameter("yearStart", year - 2);
        translatedBookQuery.setParameter("yearEnd", year + 2);
        translatedBookQuery.setMaxResults(20);
        results.addAll(translatedBookQuery.getResultList());

        return results;
    }

    @Override
    public List<Media> findByTitleSimilar(String title) {
        if (title == null || title.isBlank()) {
            return List.of();
        }

        List<Media> results = new ArrayList<>();

        // 원서에서 제목 검색
        String originalQuery = """
            SELECT o FROM OriginalBook o
            WHERE LOWER(o.title) LIKE LOWER(CONCAT('%', :title, '%'))
               OR LOWER(o.originalTitle) LIKE LOWER(CONCAT('%', :title, '%'))
            """;
        TypedQuery<Media> originalBookQuery = entityManager.createQuery(originalQuery, Media.class);
        originalBookQuery.setParameter("title", title);
        originalBookQuery.setMaxResults(20);
        results.addAll(originalBookQuery.getResultList());

        // 번역서에서 제목 검색
        String translatedQuery = """
            SELECT t FROM TranslatedBook t
            WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :title, '%'))
            """;
        TypedQuery<Media> translatedBookQuery = entityManager.createQuery(translatedQuery, Media.class);
        translatedBookQuery.setParameter("title", title);
        translatedBookQuery.setMaxResults(20);
        results.addAll(translatedBookQuery.getResultList());

        return results;
    }
}
