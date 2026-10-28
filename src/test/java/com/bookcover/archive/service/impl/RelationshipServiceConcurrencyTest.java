package com.bookcover.archive.service.impl;

import com.bookcover.archive.config.JpaConfig;
import com.bookcover.archive.domain.book.*;
import com.bookcover.archive.event.EventPublisher;
import com.bookcover.archive.exception.ValidationException;
import com.bookcover.archive.service.AuditService;
import com.bookcover.archive.service.dto.RelationshipRequest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 별도 트랜잭션에서 겹치는 원서·번역서를 수정해 교착과 오래된 연결 덮어쓰기를 검증한다. */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:relationships;MODE=PostgreSQL;NON_KEYWORDS=YEAR,VALUE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "logging.level.org.hibernate.SQL=ERROR"
}, showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RelationshipServiceConcurrencyTest {
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactions;

    private BookCover cover() {
        BookCover cover = new BookCover(); cover.setImageUrl("https://example.org/cover.png");
        cover.setThumbnailUrl("https://example.org/thumb.png"); cover.setOriginalFileName("cover.png");
        cover.setCoverType(CoverType.FRONT); em.persist(cover); return cover;
    }
    private OriginalBook original(String title) {
        OriginalBook book = new OriginalBook(); book.setTitle(title); book.setOriginalTitle(title);
        book.setLanguageCode("ko"); book.setFrontCover(cover()); em.persist(book); book.getFrontCover().setBook(book); return book;
    }
    private TranslatedBook translation(OriginalBook original) {
        TranslatedBook book = new TranslatedBook(); book.setTitle("번역서"); book.setLanguageCode("en");
        book.setOriginalBook(original); book.setFrontCover(cover()); em.persist(book);
        book.getFrontCover().setBook(book); original.getTranslations().add(book); return book;
    }
    private RelationshipRequest move(UUID previous, UUID target) {
        return new RelationshipRequest(RelationshipRequest.Action.MOVE_TRANSLATION, target, previous, null, null);
    }

    private List<Throwable> concurrently(UUID[] sources, RelationshipRequest[] requests) throws Exception {
        AuditService audit = mock(AuditService.class);
        // 잘못된 잠금 집합이면 상대가 잠근 이전 원서를 두 요청이 함께 UPDATE하게 만든다.
        doAnswer(invocation -> { Thread.sleep(200); return null; }).when(audit).logUpdate(any(), any(), any(), any(), any());
        RelationshipServiceImpl service = new RelationshipServiceImpl(em, audit, mock(EventPublisher.class));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        List<Future<Throwable>> futures = new ArrayList<>();
        try {
            for (int index = 0; index < 2; index++) {
                int i = index;
                futures.add(pool.submit(() -> {
                    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin", null,
                            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
                    try {
                        ready.countDown();
                        if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("동시 요청을 시작하지 못했습니다.");
                        new TransactionTemplate(transactions).execute(status -> {
                            service.apply(sources[i], requests[i], UUID.randomUUID()); return null;
                        });
                        return null;
                    } catch (Throwable error) { return error; }
                    finally { SecurityContextHolder.clearContext(); }
                }));
            }
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue(); start.countDown();
            List<Throwable> errors = new ArrayList<>();
            for (Future<Throwable> future : futures) {
                Throwable error = future.get(15, TimeUnit.SECONDS); if (error != null) errors.add(error);
            }
            return errors;
        } finally { start.countDown(); pool.shutdownNow(); }
    }

    @Test void oppositeMovesBothCommitAndPreserveTheTwoTranslations() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactions);
        UUID[] ids = tx.execute(status -> {
            OriginalBook a = original("A"), b = original("B");
            return new UUID[]{a.getId(), b.getId(), translation(a).getId(), translation(b).getId()};
        });
        List<Throwable> errors = concurrently(new UUID[]{ids[2], ids[3]}, new RelationshipRequest[]{move(ids[0], ids[1]), move(ids[1], ids[0])});
        assertThat(errors).isEmpty();
        tx.execute(status -> {
            assertThat(em.find(TranslatedBook.class, ids[2]).getOriginalBook().getId()).isEqualTo(ids[1]);
            assertThat(em.find(TranslatedBook.class, ids[3]).getOriginalBook().getId()).isEqualTo(ids[0]);
            assertThat(em.find(OriginalBook.class, ids[0]).getTranslations()).hasSize(1);
            assertThat(em.find(OriginalBook.class, ids[1]).getTranslations()).hasSize(1);
            return null;
        });
    }

    @Test void racingMovesOfOneTranslationRejectTheStalePreview() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactions);
        UUID[] ids = tx.execute(status -> {
            OriginalBook a = original("A"), b = original("B"), c = original("C");
            return new UUID[]{a.getId(), b.getId(), c.getId(), translation(a).getId()};
        });
        List<Throwable> errors = concurrently(new UUID[]{ids[3], ids[3]}, new RelationshipRequest[]{move(ids[0], ids[1]), move(ids[0], ids[2])});
        assertThat(errors).singleElement().isInstanceOf(ValidationException.class);
        tx.execute(status -> {
            assertThat(em.find(TranslatedBook.class, ids[3]).getOriginalBook().getId()).isIn(ids[1], ids[2]);
            assertThat(em.find(OriginalBook.class, ids[0]).getTranslations()).isEmpty(); return null;
        });
    }
}
