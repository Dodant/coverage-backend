package com.bookcover.archive.service.impl;

import com.bookcover.archive.domain.book.OriginalBook;
import com.bookcover.archive.domain.common.ContentStatus;
import com.bookcover.archive.domain.report.ReportTargetType;
import com.bookcover.archive.domain.social.Comment;
import com.bookcover.archive.event.ContentEvent;
import com.bookcover.archive.event.ContentEventType;
import com.bookcover.archive.event.EventPublisher;
import com.bookcover.archive.repository.CommentRepository;
import com.bookcover.archive.repository.OriginalBookRepository;
import com.bookcover.archive.repository.RedesignRepository;
import com.bookcover.archive.repository.ReportRepository;
import com.bookcover.archive.repository.TranslatedBookRepository;
import com.bookcover.archive.repository.UserRepository;
import com.bookcover.archive.domain.book.Redesign;
import com.bookcover.archive.domain.book.TranslatedBook;
import com.bookcover.archive.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 검수 상태 변경이 검색 재색인 이벤트로 이어지는지 확인한다. */
class ModerationServiceImplTest {

    private final OriginalBookRepository originalBookRepository = mock(OriginalBookRepository.class);
    private final TranslatedBookRepository translatedBookRepository = mock(TranslatedBookRepository.class);
    private final RedesignRepository redesignRepository = mock(RedesignRepository.class);
    private final CommentRepository commentRepository = mock(CommentRepository.class);
    private final EventPublisher eventPublisher = mock(EventPublisher.class);
    private final ModerationServiceImpl service = new ModerationServiceImpl(
            mock(ReportRepository.class), mock(UserRepository.class), originalBookRepository,
            translatedBookRepository, redesignRepository, commentRepository, eventPublisher);

    @Test
    void 책을_숨기면_재색인_이벤트를_발행한다() {
        UUID id = UUID.randomUUID();
        OriginalBook book = new OriginalBook();
        when(originalBookRepository.findById(id)).thenReturn(Optional.of(book));

        service.hideContent(ReportTargetType.ORIGINAL_BOOK, id, "test");

        assertEquals(ContentStatus.HIDDEN, book.getStatus());
        ArgumentCaptor<ContentEvent> captor = ArgumentCaptor.forClass(ContentEvent.class);
        verify(eventPublisher).publishContentEvent(captor.capture());
        assertEquals(ContentEventType.ORIGINAL_BOOK_UPDATED, captor.getValue().eventType());
        assertEquals(id, captor.getValue().entityId());
    }

    @Test
    void 댓글은_재색인하지_않는다() {
        UUID id = UUID.randomUUID();
        when(commentRepository.findById(id)).thenReturn(Optional.of(new Comment()));

        service.deleteContent(ReportTargetType.COMMENT, id, "test");

        verifyNoInteractions(eventPublisher);
    }

    enum Action { HIDE, RESTORE, DELETE }

    private void apply(Action action, ReportTargetType type, UUID id) {
        switch (action) {
            case HIDE -> service.hideContent(type, id, "r");
            case RESTORE -> service.restoreContent(type, id);
            case DELETE -> service.deleteContent(type, id, "r");
        }
    }

    private ContentEventType published() {
        ArgumentCaptor<ContentEvent> captor = ArgumentCaptor.forClass(ContentEvent.class);
        verify(eventPublisher).publishContentEvent(captor.capture());
        return captor.getValue().eventType();
    }

    @ParameterizedTest
    @EnumSource(Action.class)
    void 번역서는_TRANSLATED_BOOK_UPDATED로_재색인한다(Action action) {
        UUID id = UUID.randomUUID();
        when(translatedBookRepository.findById(id)).thenReturn(Optional.of(new TranslatedBook()));

        apply(action, ReportTargetType.TRANSLATED_BOOK, id);

        assertEquals(ContentEventType.TRANSLATED_BOOK_UPDATED, published());
    }

    @ParameterizedTest
    @EnumSource(Action.class)
    void 리디자인은_REDESIGN_UPDATED로_재색인한다(Action action) {
        UUID id = UUID.randomUUID();
        when(redesignRepository.findById(id)).thenReturn(Optional.of(new Redesign()));

        apply(action, ReportTargetType.REDESIGN, id);

        assertEquals(ContentEventType.REDESIGN_UPDATED, published());
    }

    @ParameterizedTest
    @EnumSource(Action.class)
    void 원서는_ORIGINAL_BOOK_UPDATED로_재색인한다(Action action) {
        UUID id = UUID.randomUUID();
        when(originalBookRepository.findById(id)).thenReturn(Optional.of(new OriginalBook()));

        apply(action, ReportTargetType.ORIGINAL_BOOK, id);

        assertEquals(ContentEventType.ORIGINAL_BOOK_UPDATED, published());
    }

    @Test
    void 대상이_없으면_발행하지_않는다() {
        when(originalBookRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.hideContent(ReportTargetType.ORIGINAL_BOOK, UUID.randomUUID(), "r"));
        verifyNoInteractions(eventPublisher);
    }
}
