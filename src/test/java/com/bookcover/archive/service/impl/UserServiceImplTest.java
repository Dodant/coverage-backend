package com.bookcover.archive.service.impl;

import com.bookcover.archive.domain.book.Redesign;
import com.bookcover.archive.domain.common.ContentStatus;
import com.bookcover.archive.domain.user.User;
import com.bookcover.archive.domain.user.UserStatus;
import com.bookcover.archive.event.ContentEvent;
import com.bookcover.archive.event.ContentEventType;
import com.bookcover.archive.event.EventPublisher;
import com.bookcover.archive.exception.ValidationException;
import com.bookcover.archive.repository.RedesignRepository;
import com.bookcover.archive.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 회원 탈퇴의 익명화 범위와 비밀번호 확인을 확인한다. */
class UserServiceImplTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final RedesignRepository redesignRepository = mock(RedesignRepository.class);
    private final EventPublisher eventPublisher = mock(EventPublisher.class);
    private final UserServiceImpl service =
            new UserServiceImpl(userRepository, passwordEncoder, redesignRepository, eventPublisher);

    private User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("me@test.com");
        user.setDisplayName("실명");
        user.setPasswordHash("hash");
        user.setWebsiteUrl("https://me.example");
        user.setPortfolioUrl("https://portfolio.example");
        user.setSnsAccounts(new HashMap<>(Map.of("instagram", "@me")));
        user.setStatus(UserStatus.ACTIVE);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        return user;
    }

    @Test
    void 탈퇴하면_개인정보를_지운다() {
        User user = user();
        when(passwordEncoder.matches("pw", "hash")).thenReturn(true);
        when(passwordEncoder.encode(any())).thenReturn("random-hash");

        service.withdraw(user.getId(), "pw");

        assertEquals("deleted-" + user.getId() + "@deleted.invalid", user.getEmail());
        assertEquals("탈퇴한 회원", user.getDisplayName());
        assertEquals("random-hash", user.getPasswordHash());
        assertNull(user.getWebsiteUrl());
        assertNull(user.getPortfolioUrl());
        assertTrue(user.getSnsAccounts().isEmpty());
        assertEquals(UserStatus.DELETED, user.getStatus());
    }

    @Test
    void 비밀번호가_틀리면_탈퇴하지_않는다() {
        User user = user();
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        assertThrows(ValidationException.class, () -> service.withdraw(user.getId(), "wrong"));
        verify(userRepository, never()).save(any());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void 탈퇴하면_작성한_리디자인을_재색인해_검색의_작성자_이름을_지운다() {
        User user = user();
        when(passwordEncoder.matches("pw", "hash")).thenReturn(true);
        Redesign redesign = new Redesign();
        redesign.setId(UUID.randomUUID());
        Redesign deleted = new Redesign();
        deleted.setId(UUID.randomUUID());
        deleted.setStatus(ContentStatus.DELETED);
        when(redesignRepository.findByCreatedById(user.getId())).thenReturn(List.of(redesign, deleted));

        service.withdraw(user.getId(), "pw");

        ArgumentCaptor<ContentEvent> captor = ArgumentCaptor.forClass(ContentEvent.class);
        verify(eventPublisher).publishContentEvent(captor.capture());
        assertEquals(ContentEventType.REDESIGN_UPDATED, captor.getValue().eventType());
        assertEquals(redesign.getId(), captor.getValue().entityId());
    }
}
