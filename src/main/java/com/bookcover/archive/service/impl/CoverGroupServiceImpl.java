package com.bookcover.archive.service.impl;

import com.bookcover.archive.config.security.SecurityUtils;
import com.bookcover.archive.domain.book.BookCover;
import com.bookcover.archive.exception.ResourceNotFoundException;
import com.bookcover.archive.exception.ValidationException;
import com.bookcover.archive.repository.BookCoverRepository;
import com.bookcover.archive.service.CoverGroupService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 비슷한 표지 묶음 변경. 묶음 테이블 없이 표지의 similarGroupId 값으로만 묶는다.
 *
 * Requirements: (2026-10 신규) 백오피스에서 관리자가 언어별 표지를 비슷한 디자인끼리 묶는다
 */
@Service
@Transactional
public class CoverGroupServiceImpl implements CoverGroupService {

    private final BookCoverRepository covers;

    public CoverGroupServiceImpl(BookCoverRepository covers) {
        this.covers = covers;
    }

    @Override
    public BookCover moveToGroup(UUID coverId, UUID withCoverId) {
        if (!SecurityUtils.hasRole("ADMIN")) {
            throw new ValidationException("관리자만 표지를 묶을 수 있습니다", Map.of());
        }
        if (coverId.equals(withCoverId)) {
            throw new ValidationException("같은 표지끼리는 묶을 수 없습니다", Map.of("withCoverId", withCoverId));
        }
        BookCover cover = find(coverId);
        UUID previous = cover.getSimilarGroupId();
        if (withCoverId == null) {
            cover.setSimilarGroupId(null);
        } else {
            BookCover with = find(withCoverId);
            if (with.getSimilarGroupId() == null) {
                with.setSimilarGroupId(UUID.randomUUID());
            }
            cover.setSimilarGroupId(with.getSimilarGroupId());
        }
        if (previous != null && !previous.equals(cover.getSimilarGroupId())) {
            // 위 변경이 먼저 flush되므로 옮긴 표지는 결과에 없다. 1개만 남으면 묶음이 아니다
            List<BookCover> left = covers.findBySimilarGroupId(previous);
            if (left.size() == 1) {
                left.get(0).setSimilarGroupId(null);
            }
        }
        return cover;
    }

    private BookCover find(UUID id) {
        return covers.findById(id).orElseThrow(() -> new ResourceNotFoundException("BookCover", id));
    }
}
