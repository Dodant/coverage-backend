package com.bookcover.archive.service.impl;

import com.bookcover.archive.domain.book.Redesign;
import com.bookcover.archive.domain.common.ContentStatus;
import com.bookcover.archive.domain.user.User;
import com.bookcover.archive.domain.user.UserRole;
import com.bookcover.archive.event.EventPublisher;
import com.bookcover.archive.repository.OriginalBookRepository;
import com.bookcover.archive.repository.RedesignRepository;
import com.bookcover.archive.repository.TranslatedBookRepository;
import com.bookcover.archive.repository.UserRepository;
import com.bookcover.archive.service.ContributionService;
import com.bookcover.archive.service.ImageService;
import com.bookcover.archive.service.dto.UpdateRedesignRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 리디자인 수정·삭제는 작성자 또는 ADMIN만 할 수 있는지 확인한다.
 */
class RedesignServiceImplTest {

    private final RedesignRepository redesignRepository = mock(RedesignRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final EventPublisher eventPublisher = mock(EventPublisher.class);
    private RedesignServiceImpl service;
    private Redesign redesign;
    private final UUID creatorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new RedesignServiceImpl(redesignRepository, mock(OriginalBookRepository.class),
                mock(TranslatedBookRepository.class), userRepository, mock(ContributionService.class),
                eventPublisher, mock(ImageService.class));
        User creator = new User();
        creator.setId(creatorId);
        redesign = new Redesign();
        redesign.setId(UUID.randomUUID());
        redesign.setCreatedBy(creator);
        redesign.setDescription("원래 설명");
        when(redesignRepository.findById(redesign.getId())).thenReturn(Optional.of(redesign));
        when(redesignRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void 작성자가_아니면_수정을_거절한다() {
        UUID other = UUID.randomUUID();

        assertThatThrownBy(() -> service.updateRedesign(redesign.getId(),
                new UpdateRedesignRequest(null, null, "바꾼 설명"), other))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(redesign.getDescription()).isEqualTo("원래 설명");
        verify(redesignRepository, never()).save(any());
    }

    @Test
    void 작성자는_수정할_수_있다() {
        service.updateRedesign(redesign.getId(), new UpdateRedesignRequest(null, null, "바꾼 설명"), creatorId);

        assertThat(redesign.getDescription()).isEqualTo("바꾼 설명");
    }

    @Test
    void 작성자가_아니면_삭제를_거절한다() {
        assertThatThrownBy(() -> service.deleteRedesign(redesign.getId(), UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(redesign.getStatus()).isEqualTo(ContentStatus.PUBLISHED);
    }

    @Test
    void ADMIN은_남의_리디자인을_삭제할_수_있다() {
        UUID admin = UUID.randomUUID();
        when(userRepository.existsByIdAndRole(admin, UserRole.ADMIN)).thenReturn(true);

        service.deleteRedesign(redesign.getId(), admin);

        assertThat(redesign.getStatus()).isEqualTo(ContentStatus.DELETED);
    }
}
