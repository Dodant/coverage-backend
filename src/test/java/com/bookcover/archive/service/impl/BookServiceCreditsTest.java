package com.bookcover.archive.service.impl;

import com.bookcover.archive.domain.book.BookCover;
import com.bookcover.archive.domain.book.CoverCredit;
import com.bookcover.archive.domain.book.CreditRole;
import com.bookcover.archive.domain.book.OriginalBook;
import com.bookcover.archive.domain.contributor.Designer;
import com.bookcover.archive.event.EventPublisher;
import com.bookcover.archive.exception.ValidationException;
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
import com.bookcover.archive.service.ContributionService;
import com.bookcover.archive.service.dto.AddCoverRequest;
import com.bookcover.archive.service.dto.CreditInfo;
import com.bookcover.archive.service.dto.UpdateOriginalBookRequest;
import com.bookcover.archive.validation.IsbnValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 표지 크레딧 반영(applyCredits) 규칙: 중복 제거, null이면 유지, 빈 목록이면 전부 삭제, 잘못된 입력은 400.
 */
class BookServiceCreditsTest {

    private final OriginalBookRepository originalBookRepository = mock(OriginalBookRepository.class);
    private final DesignerRepository designerRepository = mock(DesignerRepository.class);
    private final BookCoverRepository bookCoverRepository = mock(BookCoverRepository.class);
    private final List<Designer> designers = new ArrayList<>();
    private BookServiceImpl service;
    private OriginalBook book;

    @BeforeEach
    void setUp() {
        service = new BookServiceImpl(originalBookRepository, mock(TranslatedBookRepository.class),
                mock(AuthorRepository.class), mock(PublisherRepository.class), designerRepository,
                mock(TranslatorRepository.class), mock(WorkRepository.class), mock(UserRepository.class),
                mock(BookSearchRepository.class), mock(IsbnValidator.class), mock(ContributionService.class),
                mock(EventPublisher.class), bookCoverRepository, mock(SeriesRepository.class));

        book = new OriginalBook();
        book.setId(UUID.randomUUID());
        BookCover front = new BookCover();
        front.setId(UUID.randomUUID());
        book.setFrontCover(front);
        when(originalBookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(originalBookRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bookCoverRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // 이름으로 찾는 디자이너는 메모리 목록에서 찾고, 없으면 id를 붙여 만든다
        when(designerRepository.findExactNameOrAlias(anyString())).thenAnswer(inv -> designers.stream()
                .filter(d -> d.getName().equals(inv.getArgument(0))).toList());
        when(designerRepository.save(any())).thenAnswer(inv -> {
            Designer d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            designers.add(d);
            return d;
        });
    }

    @Test
    void duplicateCreditsAreStoredOnce() {
        BookCover cover = addCover(credit("가", CreditRole.DESIGN), credit("가", CreditRole.DESIGN),
                credit("가", CreditRole.ILLUSTRATION));

        assertThat(keys(cover)).containsExactlyInAnyOrder("가:DESIGN", "가:ILLUSTRATION");
    }

    @Test
    void nullCreditsKeepExistingAndEmptyListClearsAll() {
        updateFrontCredits(List.of(credit("가", CreditRole.DESIGN), credit("나", CreditRole.PHOTOGRAPHY)));
        CoverCredit kept = book.getFrontCover().getCredits().get(0);

        updateFrontCredits(null);
        assertThat(keys(book.getFrontCover())).containsExactlyInAnyOrder("가:DESIGN", "나:PHOTOGRAPHY");

        // 그대로인 크레딧은 같은 엔티티를 유지한다 (지우고 다시 넣으면 flush 순서 때문에 유니크 제약에 걸린다)
        updateFrontCredits(List.of(credit("가", CreditRole.DESIGN)));
        assertThat(book.getFrontCover().getCredits()).containsExactly(kept);

        updateFrontCredits(List.of());
        assertThat(book.getFrontCover().getCredits()).isEmpty();
    }

    @Test
    void invalidCreditsAreRejectedAsValidationErrors() {
        assertThatThrownBy(() -> addCover((CreditInfo) null)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> addCover(new CreditInfo(null, "가", null))).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> addCover(new CreditInfo(null, " ", CreditRole.DESIGN))).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> addCover(credit("가".repeat(256), CreditRole.DESIGN))).isInstanceOf(ValidationException.class);
        CreditInfo[] tooMany = IntStream.range(0, 21).mapToObj(i -> credit("d" + i, CreditRole.DESIGN)).toArray(CreditInfo[]::new);
        assertThatThrownBy(() -> addCover(tooMany)).isInstanceOf(ValidationException.class);
        assertThat(designers).isEmpty();
    }

    private static CreditInfo credit(String name, CreditRole role) {
        return new CreditInfo(null, name, role);
    }

    private BookCover addCover(CreditInfo... credits) {
        return service.addCover(book.getId(), new AddCoverRequest("i", "t", "f", null, null, null, null, null,
                null, null, null, null, null, Arrays.asList(credits)), null);
    }

    private void updateFrontCredits(List<CreditInfo> credits) {
        UpdateOriginalBookRequest.BookCoverUpdateInfo front = new UpdateOriginalBookRequest.BookCoverUpdateInfo(
                null, null, null, null, null, null, null, null, credits == null ? null : Collections.unmodifiableList(credits));
        service.updateOriginalBook(book.getId(), new UpdateOriginalBookRequest(null, null, null, null, null, null,
                null, null, null, null, null, null, null, front, null));
    }

    private static List<String> keys(BookCover cover) {
        return cover.getCredits().stream().map(c -> c.getDesigner().getName() + ":" + c.getRole()).toList();
    }
}
