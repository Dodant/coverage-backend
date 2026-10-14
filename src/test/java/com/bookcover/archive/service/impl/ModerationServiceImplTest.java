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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 검수 상태 변경이 검색 재색인 이벤트로 이어지는지 확인한다. */
class ModerationServiceImplTest {

    private final OriginalBookRepository originalBookRepository = mock(OriginalBookRepository.class);
    private final CommentRepository commentRepository = mock(CommentRepository.class);
    private final EventPublisher eventPublisher = mock(EventPublisher.class);
    private final ModerationServiceImpl service = new ModerationServiceImpl(
            mock(ReportRepository.class), mock(UserRepository.class), originalBookRepository,
            mock(TranslatedBookRepository.class), mock(RedesignRepository.class), commentRepository, eventPublisher);

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
}
