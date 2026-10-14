package com.bookcover.archive.service.impl;

import com.bookcover.archive.domain.book.OriginalBook;
import com.bookcover.archive.domain.book.TranslatedBook;
import com.bookcover.archive.domain.common.ContentStatus;
import com.bookcover.archive.domain.contributor.Author;
import com.bookcover.archive.domain.user.User;
import com.bookcover.archive.event.ContentEvent;
import com.bookcover.archive.event.ContentEventType;
import com.bookcover.archive.event.EventPublisher;
import com.bookcover.archive.exception.DuplicateIsbnException;
import com.bookcover.archive.repository.AuthorRepository;
import com.bookcover.archive.repository.BookCoverRepository;
import com.bookcover.archive.repository.BookSearchRepository;
import com.bookcover.archive.repository.DesignerRepository;
import com.bookcover.archive.repository.OriginalBookRepository;
import com.bookcover.archive.repository.PublisherRepository;
import com.bookcover.archive.repository.SeriesRepository;
import com.bookcover.archive.repository.TranslatedBookRepository;
import com.bookcover.archive.repository.TranslatorRepository;
import com.bookcover.archive.repository.UserRepository;
import com.bookcover.archive.repository.WorkRepository;
import com.bookcover.archive.service.AuditService;
import com.bookcover.archive.service.ContributionService;
import com.bookcover.archive.service.dto.CreateOriginalBookRequest;
import com.bookcover.archive.service.dto.UpdateOriginalBookRequest;
import com.bookcover.archive.validation.IsbnValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 책 등록·수정·삭제 공통 헬퍼(resolveAuthor, validateNewIsbn, softDelete)가 기존 동작을 유지하는지 확인한다.
 */
class BookServiceImplTest {

    private final OriginalBookRepository originalBookRepository = mock(OriginalBookRepository.class);
    private final TranslatedBookRepository translatedBookRepository = mock(TranslatedBookRepository.class);
    private final AuthorRepository authorRepository = mock(AuthorRepository.class);
    private final IsbnValidator isbnValidator = new IsbnValidator();
    private final ContributionService contributionService = mock(ContributionService.class);
    private final EventPublisher eventPublisher = mock(EventPublisher.class);
    private BookServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BookServiceImpl(originalBookRepository, translatedBookRepository,
                authorRepository, mock(PublisherRepository.class), mock(DesignerRepository.class),
                mock(TranslatorRepository.class), mock(WorkRepository.class), mock(UserRepository.class),
                mock(BookSearchRepository.class), isbnValidator, contributionService,
                eventPublisher, mock(BookCoverRepository.class), mock(SeriesRepository.class),
                mock(AuditService.class));
        when(originalBookRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void 이름으로_등록하면_기존_작가를_재사용한다() {
        Author existing = new Author();
        existing.setName("한강");
        when(authorRepository.findByName("한강")).thenReturn(Optional.of(existing));

        OriginalBook book = service.createOriginalBook(new CreateOriginalBookRequest(
                "채식주의자", "채식주의자", "KR", "ko", 2007, "소설", null,
                null, "한강", null, null, null, null, null, null, null,
                new CreateOriginalBookRequest.BookCoverInfo("http://x/a.jpg", "http://x/a_t.jpg", "a.jpg",
                        null, null, null, null, null, null),
                null, null));

        assertThat(book.getAuthor()).isSameAs(existing);
        verify(authorRepository, never()).save(any());
    }

    @Test
    void 수정_시_다른_책의_ISBN이면_중복_예외() {
        OriginalBook book = new OriginalBook();
        book.setId(UUID.randomUUID());
        when(originalBookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        TranslatedBook other = new TranslatedBook();
        other.setId(UUID.randomUUID());
        when(translatedBookRepository.findByIsbn("9788936433598")).thenReturn(Optional.of(other));

        UpdateOriginalBookRequest request = new UpdateOriginalBookRequest(null, null, null, null, null, null,
                "978-89-364-3359-8", null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.updateOriginalBook(book.getId(), request, UUID.randomUUID()))
                .isInstanceOf(DuplicateIsbnException.class);
    }

    @Test
    void 삭제하면_DELETED로_바꾸고_기여를_줄이고_삭제_이벤트를_발행한다() {
        User creator = new User();
        creator.setId(UUID.randomUUID());
        TranslatedBook book = new TranslatedBook();
        book.setId(UUID.randomUUID());
        book.setCreatedBy(creator);
        when(translatedBookRepository.findById(book.getId())).thenReturn(Optional.of(book));

        service.deleteTranslatedBook(book.getId());

        assertThat(book.getStatus()).isEqualTo(ContentStatus.DELETED);
        verify(contributionService).decrementContribution(creator.getId());
        ArgumentCaptor<ContentEvent> event = ArgumentCaptor.forClass(ContentEvent.class);
        verify(eventPublisher).publishContentEvent(event.capture());
        assertThat(event.getValue().eventType()).isEqualTo(ContentEventType.TRANSLATED_BOOK_DELETED);
        assertThat(event.getValue().entityId()).isEqualTo(book.getId());
        assertThat(event.getValue().userId()).isEqualTo(creator.getId());
    }

    @Test
    void 이미_삭제된_책은_다시_처리하지_않는다() {
        OriginalBook book = new OriginalBook();
        book.setId(UUID.randomUUID());
        book.setStatus(ContentStatus.DELETED);
        when(originalBookRepository.findById(book.getId())).thenReturn(Optional.of(book));

        service.deleteOriginalBook(book.getId());

        verifyNoInteractions(contributionService, eventPublisher);
    }
}
