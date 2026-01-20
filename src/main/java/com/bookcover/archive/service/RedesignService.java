package com.bookcover.archive.service;

import com.bookcover.archive.domain.book.Redesign;
import com.bookcover.archive.service.dto.CreateRedesignRequest;
import com.bookcover.archive.service.dto.UpdateRedesignRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * 리디자인 관리 서비스 인터페이스.
 * 리디자인 CRUD 및 회원 연결 기능을 제공합니다.
 *
 * Requirements:
 * - 3.1: 리디자인을 업로드하면 해당 리디자인을 원서 또는 번역서와 연결
 * - 3.2: 리디자인 이미지, 설명을 필수 항목으로 검증
 * - 3.5: 한 책에 대해 여러 회원이 각각 리디자인을 업로드하는 것을 허용
 * - 3.6: 한 회원이 같은 책에 대해 여러 개의 리디자인을 업로드하는 것을 허용
 */
public interface RedesignService {

    Redesign createRedesign(CreateRedesignRequest request);
    Redesign updateRedesign(UUID id, UpdateRedesignRequest request);
    Optional<Redesign> getRedesign(UUID id);
    void deleteRedesign(UUID id);

    Page<Redesign> getRedesignsByOriginalBook(UUID originalBookId, Pageable pageable);
    Page<Redesign> getRedesignsByTranslatedBook(UUID translatedBookId, Pageable pageable);
    Page<Redesign> getRedesignsByUser(UUID userId, Pageable pageable);
}
