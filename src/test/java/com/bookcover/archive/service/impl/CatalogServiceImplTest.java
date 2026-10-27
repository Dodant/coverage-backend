package com.bookcover.archive.service.impl;

import com.bookcover.archive.config.JpaConfig;
import com.bookcover.archive.domain.book.*;
import com.bookcover.archive.domain.common.ContentStatus;
import com.bookcover.archive.exception.ValidationException;
import com.bookcover.archive.repository.BookCoverRepository;
import com.bookcover.archive.service.AuditService;
import com.bookcover.archive.service.CatalogService;
import com.bookcover.archive.service.CatalogService.*;
import com.bookcover.archive.service.dto.UpdateCoverMetadataRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.context.annotation.Import;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** DB에서 전체 종류·상태를 필터링한 뒤 페이징하는 계약과 표지 수정 소유권을 검증한다. */
@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:catalog;MODE=PostgreSQL;NON_KEYWORDS=YEAR,VALUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=false"
}, showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class CatalogServiceImplTest {
    @Autowired EntityManager em;
    @Autowired BookCoverRepository covers;
    private CatalogService service;
    private final AuditService audit = mock(AuditService.class);
    private final com.bookcover.archive.event.EventPublisher events = mock(com.bookcover.archive.event.EventPublisher.class);

    @BeforeEach void setup() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        service = new CatalogServiceImpl(em, covers, audit, new ObjectMapper().registerModule(new JavaTimeModule()), events);
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    private OriginalBook original(String title, ContentStatus status) {
        OriginalBook b = new OriginalBook();
        b.setTitle(title); b.setOriginalTitle(title); b.setLanguageCode("ko"); b.setStatus(status);
        b.setYear(2026); b.setIsbn("9780000000002");
        BookCover c = cover(); b.setFrontCover(c);
        em.persist(b); c.setBook(b); b.getCovers().add(c);
        return b;
    }
    private BookCover cover() {
        BookCover c = new BookCover(); c.setImageUrl("https://stored/cover.jpg"); c.setThumbnailUrl("https://stored/thumb.jpg");
        c.setOriginalFileName("cover.jpg"); c.setCoverType(CoverType.FRONT); c.setWidth(800); c.setHeight(1200);
        em.persist(c); return c;
    }
    private TranslatedBook translation(OriginalBook original) {
        TranslatedBook b = new TranslatedBook(); b.setTitle("English edition"); b.setLanguageCode("en");
        b.setIsbn("9780000000019"); b.setOriginalBook(original); b.setStatus(ContentStatus.HIDDEN);
        BookCover c = cover(); b.setFrontCover(c); em.persist(b); c.setBook(b); b.getCovers().add(c);
        original.getTranslations().add(b); return b;
    }
    private Filter filter(Issue issue) { return new Filter("", null, null, "", "", null, issue, false); }
    private void flush() { em.flush(); em.clear(); }

    @Test void includesUnpublishedTranslationsAndPaginatesAfterFiltering() {
        OriginalBook o = original("한글 원서", ContentStatus.DRAFT); TranslatedBook t = translation(o); flush();
        var all = service.list(filter(null), 0, 1, "title");
        assertThat(all.getTotalElements()).isEqualTo(2);
        assertThat(all.getTotalPages()).isEqualTo(2);
        var translated = service.list(new Filter("English", Kind.TRANSLATED, ContentStatus.HIDDEN, "en", "", null, null, false), 0, 1, "title");
        assertThat(translated.getTotalElements()).isEqualTo(1);
        assertThat(translated.getContent().getFirst().id()).isEqualTo(t.getId());
        var detail = service.detail(o.getId());
        assertThat(detail.relatedBooks()).hasSize(2);
        assertThat(service.editions(o.getId())).hasSize(2);
    }

    @Test void sqlIssueFiltersMatchDisplayedIssuesIncludingUnknownAndHiddenImages() {
        OriginalBook o = original("표지 점검", ContentStatus.PUBLISHED);
        o.setIsbn(null); o.setYear(null); o.getFrontCover().setWidth(null);
        BookCover variant = cover(); variant.setBook(o); o.getCovers().add(variant); flush();
        for (Issue issue : Issue.values()) {
            var detail = service.detail(o.getId());
            assertThat(service.list(filter(issue), 0, 30, "created").getTotalElements())
                    .as(issue.name()).isEqualTo(detail.issues().contains(issue) ? 1 : 0);
        }
        BookCover c = em.find(OriginalBook.class, o.getId()).getFrontCover(); c.setHidden(true); flush();
        assertThat(service.list(filter(Issue.HIDDEN_COVER), 0, 30, "created").getTotalElements()).isEqualTo(1);
        assertThat(service.list(filter(Issue.UNKNOWN_RESOLUTION), 0, 30, "created").getTotalElements()).isZero();
        assertThat(service.list(new Filter("", null, null, "", "", false, null, false), 0, 30, "created").getTotalElements()).isEqualTo(1);
    }

    @Test void exactResolutionBoundaryAndLiteralSearchDoNotBecomeWildcards() {
        OriginalBook o = original("100%_책", ContentStatus.PUBLISHED); flush();
        assertThat(service.list(filter(Issue.LOW_RESOLUTION), 0, 30, "created").getTotalElements()).isZero();
        assertThat(service.list(new Filter("%_", null, null, "", "", null, null, false), 0, 30, "created").getTotalElements()).isEqualTo(1);
        assertThat(service.list(new Filter("978-0000000002", null, null, "", "", null, null, false), 0, 30, "created").getTotalElements()).isEqualTo(1);
        em.find(OriginalBook.class, o.getId()).getFrontCover().setWidth(799); flush();
        assertThat(service.list(filter(Issue.LOW_RESOLUTION), 0, 30, "created").getTotalElements()).isEqualTo(1);
    }

    @Test void metadataPersistsAndCannotModifyAnotherBooksCover() {
        OriginalBook o = original("첫 도서", ContentStatus.DRAFT);
        TranslatedBook t = translation(o);
        UUID coverId = o.getFrontCover().getId(); flush();
        var request = new UpdateCoverMetadataRequest("10쇄 리커버", 10, LocalDate.of(2026, 10, 1), CoverVariantType.RECOVER,
                RibbonStatus.CLEAN, "출판사", "https://publisher.example/book", null);
        service.updateCover(o.getId(), coverId, request, UUID.randomUUID()); flush();
        var c = em.find(BookCover.class, coverId);
        assertThat(c.getPrintingFrom()).isEqualTo(10); assertThat(c.getVariantType()).isEqualTo(CoverVariantType.RECOVER);
        assertThat(c.getRibbonStatus()).isEqualTo(RibbonStatus.CLEAN);
        assertThat(c.getUsedFrom()).isEqualTo(LocalDate.of(2026, 10, 1));
        verify(audit).logUpdate(any(), eq(o.getId()), any(), contains("UNKNOWN"), contains("RECOVER"));
        assertThatThrownBy(() -> service.updateCover(t.getId(), coverId, request, UUID.randomUUID())).isInstanceOf(ValidationException.class);
        service.updateCover(o.getId(), coverId, new UpdateCoverMetadataRequest("", null, null, CoverVariantType.UNKNOWN, RibbonStatus.UNKNOWN, "", "", null), UUID.randomUUID()); flush();
        assertThat(em.find(BookCover.class, coverId).getPrintingFrom()).isNull();
        assertThat(em.find(BookCover.class, coverId).getSourceUrl()).isNull();
    }

    @Test void rejectsNonAdminAndDeletedMutation() {
        OriginalBook o = original("삭제", ContentStatus.DELETED); UUID coverId = o.getFrontCover().getId(); flush();
        var request = new UpdateCoverMetadataRequest(null, null, null, CoverVariantType.UNKNOWN, RibbonStatus.UNKNOWN, null, null, null);
        assertThatThrownBy(() -> service.updateCover(o.getId(), coverId, request, UUID.randomUUID())).isInstanceOf(ValidationException.class);
        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> service.list(filter(null), 0, 30, "created")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.detail(o.getId())).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.updateCover(o.getId(), coverId, request, UUID.randomUUID())).isInstanceOf(ValidationException.class);
    }

    @Test void creditsCanBeAddedChangedAndClearedWithoutDuplicatingAssignments() {
        OriginalBook o = original("크레딧", ContentStatus.PUBLISHED);
        var designer = new com.bookcover.archive.domain.contributor.Designer(); designer.setName("김디자인"); em.persist(designer);
        UUID coverId = o.getFrontCover().getId(); UUID designerId = designer.getId(); flush();
        var credit = new UpdateCoverMetadataRequest.CreditAssignment(designerId, CreditRole.DESIGN);
        var request = new UpdateCoverMetadataRequest(null, null, null, CoverVariantType.INITIAL, RibbonStatus.CLEAN, null, null, List.of(credit, credit));
        service.updateCover(o.getId(), coverId, request, UUID.randomUUID()); flush();
        assertThat(em.find(BookCover.class, coverId).getCredits()).hasSize(1);
        verify(events).publishContentEvent(argThat(event -> event.entityId().equals(o.getId())));
        assertThat(service.list(filter(Issue.MISSING_CREDIT), 0, 30, "created").getTotalElements()).isZero();
        service.updateCover(o.getId(), coverId, request, UUID.randomUUID()); flush();
        assertThat(em.find(BookCover.class, coverId).getCredits()).hasSize(1);
        var changed = new UpdateCoverMetadataRequest(null, null, null, CoverVariantType.INITIAL, RibbonStatus.CLEAN, null, null,
                List.of(new UpdateCoverMetadataRequest.CreditAssignment(designerId, CreditRole.ILLUSTRATION)));
        service.updateCover(o.getId(), coverId, changed, UUID.randomUUID()); flush();
        assertThat(em.find(BookCover.class, coverId).getCredits().getFirst().getRole()).isEqualTo(CreditRole.ILLUSTRATION);
        service.updateCover(o.getId(), coverId, new UpdateCoverMetadataRequest(null, null, null, CoverVariantType.UNKNOWN, RibbonStatus.UNKNOWN, null, null, List.of()), UUID.randomUUID()); flush();
        assertThat(service.list(filter(Issue.MISSING_CREDIT), 0, 30, "created").getTotalElements()).isEqualTo(1);
    }
}
