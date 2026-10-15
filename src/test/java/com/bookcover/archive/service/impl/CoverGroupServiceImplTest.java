package com.bookcover.archive.service.impl;

import com.bookcover.archive.domain.book.BookCover;
import com.bookcover.archive.exception.ValidationException;
import com.bookcover.archive.repository.BookCoverRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CoverGroupServiceImplTest {

    private final BookCoverRepository repo = mock(BookCoverRepository.class);
    private final CoverGroupServiceImpl service = new CoverGroupServiceImpl(repo);
    private final Map<UUID, BookCover> db = new HashMap<>();

    @BeforeEach
    void setUp() {
        when(repo.findById(any())).thenAnswer(i -> Optional.ofNullable(db.get(i.<UUID>getArgument(0))));
        // JPA가 쿼리 전에 변경을 flush하는 것처럼 현재 값으로 찾는다
        when(repo.findBySimilarGroupId(any())).thenAnswer(i -> {
            List<BookCover> out = new ArrayList<>();
            db.values().forEach(c -> { if (i.getArgument(0).equals(c.getSimilarGroupId())) out.add(c); });
            return out;
        });
        admin(true);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void admin(boolean isAdmin) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "u", null, List.of(new SimpleGrantedAuthority(isAdmin ? "ROLE_ADMIN" : "ROLE_MEMBER"))));
    }

    private BookCover cover() {
        BookCover c = new BookCover();
        c.setId(UUID.randomUUID());
        db.put(c.getId(), c);
        return c;
    }

    @Test
    void groupsMovesAndDissolvesSingletons() {
        BookCover en = cover(), de = cover(), nl = cover();

        service.moveToGroup(de.getId(), en.getId());          // de → en: 새 묶음
        assertThat(en.getSimilarGroupId()).isNotNull().isEqualTo(de.getSimilarGroupId());

        service.moveToGroup(nl.getId(), de.getId());          // nl → 같은 묶음
        assertThat(nl.getSimilarGroupId()).isEqualTo(en.getSimilarGroupId());

        service.moveToGroup(de.getId(), null);                // de 빼기: en, nl은 남는다
        assertThat(de.getSimilarGroupId()).isNull();
        assertThat(en.getSimilarGroupId()).isNotNull().isEqualTo(nl.getSimilarGroupId());

        service.moveToGroup(nl.getId(), null);                // nl 빼기: en 혼자 → 묶음 풀림
        assertThat(en.getSimilarGroupId()).isNull();
    }

    @Test
    void movingToAnotherGroupDissolvesTheOldPair() {
        BookCover a = cover(), b = cover(), c = cover();
        service.moveToGroup(b.getId(), a.getId());
        service.moveToGroup(b.getId(), c.getId());            // b가 c에게 가면 a는 혼자
        assertThat(a.getSimilarGroupId()).isNull();
        assertThat(b.getSimilarGroupId()).isNotNull().isEqualTo(c.getSimilarGroupId());
    }

    @Test
    void rejectsSelfAndNonAdmin() {
        BookCover a = cover(), b = cover();
        assertThrows(ValidationException.class, () -> service.moveToGroup(a.getId(), a.getId()));
        admin(false);
        assertThrows(ValidationException.class, () -> service.moveToGroup(a.getId(), b.getId()));
        assertThat(a.getSimilarGroupId()).isNull();
    }
}
