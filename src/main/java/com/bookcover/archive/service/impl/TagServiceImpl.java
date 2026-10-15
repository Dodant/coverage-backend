package com.bookcover.archive.service.impl;

import com.bookcover.archive.config.security.SecurityUtils;
import com.bookcover.archive.domain.book.BookCover;
import com.bookcover.archive.domain.book.OriginalBook;
import com.bookcover.archive.domain.media.Media;
import com.bookcover.archive.domain.tag.DesignTag;
import com.bookcover.archive.domain.tag.TagCategory;
import com.bookcover.archive.event.ContentEvent;
import com.bookcover.archive.event.ContentEventType;
import com.bookcover.archive.event.EventPublisher;
import com.bookcover.archive.exception.ResourceNotFoundException;
import com.bookcover.archive.exception.ValidationException;
import com.bookcover.archive.repository.BookCoverRepository;
import com.bookcover.archive.repository.DesignTagRepository;
import com.bookcover.archive.repository.OriginalBookRepository;
import com.bookcover.archive.repository.TranslatedBookRepository;
import com.bookcover.archive.service.TagService;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 태그 변경을 DB와 검색 문서에 반영한다. Requirements: 20.1, 20.3, 20.4. */
@Service
@Transactional
public class TagServiceImpl implements TagService {
    private final DesignTagRepository tags;
    private final BookCoverRepository covers;
    private final OriginalBookRepository originals;
    private final TranslatedBookRepository translations;
    private final EventPublisher events;

    public TagServiceImpl(DesignTagRepository tags, BookCoverRepository covers,
                          OriginalBookRepository originals, TranslatedBookRepository translations,
                          EventPublisher events) {
        this.tags = tags;
        this.covers = covers;
        this.originals = originals;
        this.translations = translations;
        this.events = events;
    }

    @Override
    public DesignTag createTag(String name, TagCategory category) {
        requireAdmin();
        if (name == null || name.isBlank() || category == null) {
            throw new ValidationException("태그 이름과 분류가 필요합니다", Map.of());
        }
        String normalized = name.trim();
        if (tags.existsByName(normalized)) {
            throw new ValidationException("이미 존재하는 태그입니다", Map.of("name", normalized));
        }
        DesignTag tag = new DesignTag();
        tag.setName(normalized);
        tag.setCategory(category);
        return tags.save(tag);
    }

    @Override
    public void assignTag(UUID coverId, UUID tagId) {
        requireAdmin();
        BookCover cover = cover(coverId);
        DesignTag tag = tags.findById(tagId).orElseThrow(() -> new ResourceNotFoundException("DesignTag", tagId));
        if (!Boolean.TRUE.equals(tag.getApproved())) {
            throw new ValidationException("승인된 태그만 부여할 수 있습니다", Map.of("tagId", tagId));
        }
        if (cover.getTags().add(tag)) {
            covers.save(cover);
            tags.incrementUsage(tagId);
            reindex(cover);
        }
    }

    @Override
    public void removeTag(UUID coverId, UUID tagId) {
        requireAdmin();
        BookCover cover = cover(coverId);
        DesignTag tag = tags.findById(tagId).orElseThrow(() -> new ResourceNotFoundException("DesignTag", tagId));
        if (cover.getTags().removeIf(t -> t.getId().equals(tag.getId()))) {
            covers.save(cover);
            tags.decrementUsage(tagId);
            reindex(cover);
        }
    }

    private BookCover cover(UUID id) {
        BookCover cover = covers.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("BookCover", id));
        if (cover.isHidden()) throw new ResourceNotFoundException("BookCover", id);
        return cover;
    }

    private void reindex(BookCover cover) {
        Media book = cover.getBook();
        if (book == null) {
            book = originals.findByBackCoverId(cover.getId()).<Media>map(value -> value)
                    .or(() -> translations.findByBackCoverId(cover.getId()))
                    .orElseThrow(() -> new ResourceNotFoundException("BookForCover", cover.getId()));
        }
        ContentEventType type = book instanceof OriginalBook
                ? ContentEventType.ORIGINAL_BOOK_UPDATED : ContentEventType.TRANSLATED_BOOK_UPDATED;
        events.publishContentEvent(new ContentEvent(type, book.getId(), null));
    }

    private static void requireAdmin() {
        if (!SecurityUtils.hasRole("ADMIN")) {
            throw new ValidationException("관리자만 태그를 변경할 수 있습니다", Map.of());
        }
    }
}
