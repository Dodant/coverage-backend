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
    private com.bookcover.archive.service.RelationshipService relationships() {
        return new RelationshipServiceImpl(em, audit, events);
    }
    private com.bookcover.archive.service.dto.RelationshipRequest relation(com.bookcover.archive.service.dto.RelationshipRequest.Action action, UUID target,
            com.bookcover.archive.service.RelationshipService.Preview preview) {
        return new com.bookcover.archive.service.dto.RelationshipRequest(action, target,
                preview == null ? null : preview.originalId(), preview == null ? null : preview.workId(), preview == null ? null : preview.targetWorkId());
    }
    @Test void translationMovePreservesCoversAndRequiresFreshPreview() {
        var first = original("원서1", ContentStatus.PUBLISHED); first.setIsbn(null); em.flush();
        var second = original("원서2", ContentStatus.PUBLISHED); second.setIsbn(null);
        var translated = translation(first); UUID cover = translated.getFrontCover().getId(); flush();
        var service = relationships(); var action = com.bookcover.archive.service.dto.RelationshipRequest.Action.MOVE_TRANSLATION;
        var preview = service.preview(translated.getId(), relation(action, second.getId(), null));
        assertThat(preview.originalId()).isEqualTo(first.getId());
        assertThatThrownBy(() -> service.apply(translated.getId(), relation(action, second.getId(), null), UUID.randomUUID())).isInstanceOf(ValidationException.class);
        service.apply(translated.getId(), relation(action, second.getId(), preview), UUID.randomUUID()); flush();
        var changed = em.find(TranslatedBook.class, translated.getId());
        assertThat(changed.getOriginalBook().getId()).isEqualTo(second.getId());
        assertThat(changed.getFrontCover().getId()).isEqualTo(cover);
        assertThat(em.find(OriginalBook.class, first.getId()).getTranslations()).isEmpty();
        assertThat(em.find(OriginalBook.class, second.getId()).getTranslations()).hasSize(1);
    }
    @Test void editionsCanJoinAndLeaveAWorkWithoutMovingTranslations() {
        var first = original("초판", ContentStatus.PUBLISHED); first.setIsbn(null); em.flush();
        var second = original("개정판", ContentStatus.PUBLISHED); second.setIsbn(null);
        var translated = translation(first); flush();
        var service = relationships(); var join = com.bookcover.archive.service.dto.RelationshipRequest.Action.JOIN_WORK;
        var preview = service.preview(first.getId(), relation(join, second.getId(), null));
        assertThat(preview.affectedBooks()).hasSize(3);
        service.apply(first.getId(), relation(join, second.getId(), preview), UUID.randomUUID()); flush();
        assertThat(em.find(OriginalBook.class, first.getId()).getWork().getId()).isEqualTo(em.find(OriginalBook.class, second.getId()).getWork().getId());
        var leave = com.bookcover.archive.service.dto.RelationshipRequest.Action.LEAVE_WORK;
        var leavePreview = service.preview(first.getId(), relation(leave, null, null));
        service.apply(first.getId(), relation(leave, null, leavePreview), UUID.randomUUID()); flush();
        assertThat(em.find(OriginalBook.class, first.getId()).getWork()).isNull();
        assertThat(em.find(TranslatedBook.class, translated.getId()).getOriginalBook().getId()).isEqualTo(first.getId());
    }
    @Test void publicTranslationCannotMoveToUnpublishedOriginalAndMembersCannotPreview() {
        var first = original("공개 원서", ContentStatus.PUBLISHED); first.setIsbn(null); em.flush();
        var second = original("초안 원서", ContentStatus.DRAFT); second.setIsbn(null);
        var translated = translation(first); translated.setStatus(ContentStatus.PUBLISHED); flush();
        var request = relation(com.bookcover.archive.service.dto.RelationshipRequest.Action.MOVE_TRANSLATION, second.getId(), null);
        assertThatThrownBy(() -> relationships().preview(translated.getId(), request)).isInstanceOf(ValidationException.class);
        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> relationships().preview(translated.getId(), request)).isInstanceOf(ValidationException.class);
    }
    @Test void imageComparisonSeparatesSameUrlsExactFilesAndUnconfirmedCandidates() {
        var book = original("비교", ContentStatus.PUBLISHED);
        var a = book.getFrontCover(); a.setImageUrl("https://stored/a.png"); a.setWidth(800); a.setHeight(1200);
        var b = cover(); b.setImageUrl("https://stored/b.png"); b.setBook(book); book.getCovers().add(b);
        var c = cover(); c.setImageUrl("https://stored/c.jpg"); c.setBook(book); book.getCovers().add(c);
        for (var cover : List.of(a,b,c)) {
            var asset = new ImageAsset(); asset.setImageUrl(cover.getImageUrl()); asset.setFileSha256(cover == c ? "different" : "same");
            asset.setPerceptualHash("0000000000000000"); em.persist(asset);
        }
        flush(); var images = new ImageInspectionServiceImpl(em, mock(com.bookcover.archive.service.ImageService.class));
        var matches = images.matches(book.getId(), "", 0);
        assertThat(matches.totalMatches()).isEqualTo(3); assertThat(matches.indexedCovers()).isEqualTo(3);
        assertThat(images.matches(book.getId(), "EXACT_FILE", 0).matches()).hasSize(1);
        assertThat(images.matches(book.getId(), "SIMILAR", 0).matches()).hasSize(2);
        em.find(BookCover.class, c.getId()).setHidden(true); flush();
        assertThat(images.matches(book.getId(), "", 0).totalMatches()).isEqualTo(1);
        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> images.matches(null, "", 0)).isInstanceOf(ValidationException.class);
    }

    @Test void imageScanIsolatesFileFailuresAndReturnsTheAffectedBook() {
        var book = original("해시 분석", ContentStatus.PUBLISHED);
        book.getFrontCover().setImageUrl("https://stored/broken.png");
        var second = cover(); second.setImageUrl("https://stored/ok.png"); second.setBook(book); book.getCovers().add(second); flush();
        var reader = mock(com.bookcover.archive.service.ImageService.class);
        when(reader.indexFingerprint("https://stored/broken.png")).thenThrow(new ValidationException("원본 없음", java.util.Map.of()));
        when(reader.indexFingerprint("https://stored/ok.png")).thenReturn(true);
        var images = new ImageInspectionServiceImpl(em, reader);
        var result = images.scan(null, 10);
        assertThat(result.examined()).isEqualTo(2); assertThat(result.indexed()).isEqualTo(1); assertThat(result.hasMore()).isFalse();
        assertThat(result.failures()).hasSize(1);
        assertThat(result.failures().getFirst().bookId()).isEqualTo(book.getId());
        assertThat(result.failures().getFirst().message()).isEqualTo("원본 없음");
        assertThat(images.scan(result.nextCursor(), 10).examined()).isZero();
    }

}
