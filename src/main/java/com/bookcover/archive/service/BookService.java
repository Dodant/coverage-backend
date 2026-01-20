package com.bookcover.archive.service;

import com.bookcover.archive.domain.book.OriginalBook;
import com.bookcover.archive.domain.book.TranslatedBook;
import com.bookcover.archive.domain.media.Media;
import com.bookcover.archive.service.dto.CreateOriginalBookRequest;
import com.bookcover.archive.service.dto.CreateTranslatedBookRequest;
import com.bookcover.archive.service.dto.UpdateOriginalBookRequest;
import com.bookcover.archive.service.dto.UpdateTranslatedBookRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 책 관리 서비스 인터페이스.
 * 원서/번역서 CRUD 및 중복 검사 기능을 제공합니다.
 *
 * Requirements:
 * - 1.1: 원서 등록 시 필수 항목 검증
 * - 1.5: 원서 등록 시 고유 식별자 생성 및 저장
 * - 2.1: 번역서를 기존 원서와 연결
 * - 12.1: ISBN 중복 검사
 * - 12.4: 기존 엔트리에 새 에디션으로 추가
 */
public interface BookService {

    // 원서 관리
    OriginalBook createOriginalBook(CreateOriginalBookRequest request);
    OriginalBook updateOriginalBook(UUID id, UpdateOriginalBookRequest request);
    Optional<OriginalBook> getOriginalBook(UUID id);
    void deleteOriginalBook(UUID id);

    // 번역서 관리
    TranslatedBook createTranslatedBook(UUID originalBookId, CreateTranslatedBookRequest request);
    TranslatedBook updateTranslatedBook(UUID id, UpdateTranslatedBookRequest request);
    Optional<TranslatedBook> getTranslatedBook(UUID id);
    void deleteTranslatedBook(UUID id);

    // 조회
    List<TranslatedBook> getTranslationsByOriginalBook(UUID originalBookId);
    Page<OriginalBook> getBooksByAuthor(UUID authorId, Pageable pageable);
    Page<OriginalBook> getBooksByPublisher(UUID publisherId, Pageable pageable);
    Page<OriginalBook> getBooksByDesigner(UUID designerId, Pageable pageable);

    // 중복 검사
    List<Media> findDuplicatesByIsbn(String isbn);
    List<Media> findSimilarBooks(String title, String authorName, int year);

    // 작품 그룹화
    void linkToWork(UUID bookId, UUID workId);
}
