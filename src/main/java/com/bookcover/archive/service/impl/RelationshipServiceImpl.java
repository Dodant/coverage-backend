package com.bookcover.archive.service.impl;

import com.bookcover.archive.config.security.SecurityUtils;
import com.bookcover.archive.domain.audit.AuditEntityType;
import com.bookcover.archive.domain.book.*;
import com.bookcover.archive.domain.common.ContentStatus;
import com.bookcover.archive.domain.media.Media;
import com.bookcover.archive.event.*;
import com.bookcover.archive.exception.ResourceNotFoundException;
import com.bookcover.archive.exception.ValidationException;
import com.bookcover.archive.service.*;
import com.bookcover.archive.service.dto.RelationshipRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

/** 관계 수정은 표지·서지정보를 옮기지 않고 연결 ID만 변경한다. */
@Service
@Transactional
public class RelationshipServiceImpl implements RelationshipService {
    private final EntityManager em;
    private final AuditService audit;
    private final EventPublisher events;
    public RelationshipServiceImpl(EntityManager em, AuditService audit, EventPublisher events) {
        this.em = em; this.audit = audit; this.events = events;
    }

    @Override @Transactional(readOnly = true)
    public Preview preview(UUID id, RelationshipRequest r) {
        requireAdmin();
        Media source = find(id, false), target = r.targetId() == null ? null : find(r.targetId(), false);
        validate(source, target, r);
        OriginalBook original = source instanceof TranslatedBook t ? t.getOriginalBook() : (OriginalBook) source;
        List<CatalogService.Item> affected = new ArrayList<>();
        affected.add(CatalogServiceImpl.item(source));
        if (source instanceof OriginalBook o) o.getTranslations().forEach(t -> affected.add(CatalogServiceImpl.item(t)));
        if (r.action() == RelationshipRequest.Action.JOIN_WORK && target instanceof OriginalBook destination && destination.getWork() == null) {
            affected.add(CatalogServiceImpl.item(destination));
            destination.getTranslations().forEach(t -> affected.add(CatalogServiceImpl.item(t)));
        }
        String summary = switch (r.action()) {
            case MOVE_TRANSLATION -> "번역서의 연결 원서를 변경합니다. 번역서의 표지와 서지정보는 그대로 유지됩니다.";
            case JOIN_WORK -> "선택한 원서를 대상 원서와 같은 작품의 다른 판으로 연결합니다. 원서별 번역서와 표지는 유지됩니다.";
            case LEAVE_WORK -> "현재 원서를 작품 묶음에서 분리합니다. 연결된 번역서는 이 원서를 계속 따릅니다.";
        };
        return new Preview(CatalogServiceImpl.item(source), target == null ? null : CatalogServiceImpl.item(target),
                source instanceof TranslatedBook ? original.getId() : null, original.getWork() == null ? null : original.getWork().getId(),
                target instanceof OriginalBook o && o.getWork() != null ? o.getWork().getId() : null, affected, summary);
    }

    @Override
    public void apply(UUID id, RelationshipRequest r, UUID userId) {
        requireAdmin();
        // 번역서와 대상뿐 아니라 실제로 갱신할 이전 원서까지 같은 순서로 잠근다.
        // 아래 잠금 이후 다시 읽고 검토 ID를 검사하므로, 탐색 중 관계가 바뀌면 적용하지 않는다.
        Media observed = find(id, false);
        Set<UUID> ids = new TreeSet<>();
        ids.add(id);
        if (r.targetId() != null && !id.equals(r.targetId())) ids.add(r.targetId());
        if (observed instanceof TranslatedBook translated) ids.add(translated.getOriginalBook().getId());
        Map<UUID, Media> locked = new HashMap<>();
        for (UUID key : ids) locked.put(key, find(key, true));
        Media source = locked.get(id), target = locked.get(r.targetId());
        validate(source, target, r);
        OriginalBook original = source instanceof TranslatedBook t ? t.getOriginalBook() : (OriginalBook) source;
        if (!locked.containsKey(original.getId()))
            throw new ValidationException("검토 이후 연결 원서가 변경되었습니다. 변경 내용을 다시 확인하세요.", Map.of("relationship", "conflict"));
        UUID previousOriginal = source instanceof TranslatedBook ? original.getId() : null;
        UUID previousWork = original.getWork() == null ? null : original.getWork().getId();
        UUID targetWork = target instanceof OriginalBook o && o.getWork() != null ? o.getWork().getId() : null;
        if (!Objects.equals(previousOriginal, r.expectedOriginalId()) || !Objects.equals(previousWork, r.expectedWorkId())
                || !Objects.equals(targetWork, r.expectedTargetWorkId()))
            throw new ValidationException("검토 이후 관계가 변경되었습니다. 변경 내용을 다시 확인하세요.", Map.of("relationship", "conflict"));
        if (r.action() == RelationshipRequest.Action.MOVE_TRANSLATION) {
            TranslatedBook translated = (TranslatedBook) source;
            OriginalBook destination = (OriginalBook) target;
            original.getTranslations().remove(translated);
            destination.getTranslations().add(translated);
            translated.setOriginalBook(destination);
            audit.logUpdate(AuditEntityType.TRANSLATED_BOOK, id, userId, json("originalBookId", previousOriginal), json("originalBookId", destination.getId()));
            touch(original); touch(destination);
        } else {
            OriginalBook book = (OriginalBook) source;
            if (r.action() == RelationshipRequest.Action.LEAVE_WORK) book.setWork(null);
            else {
                OriginalBook destination = (OriginalBook) target;
                Work work = destination.getWork();
                if (work == null) {
                    work = new Work(); work.setTitle(destination.getTitle()); work.setOriginalTitle(destination.getOriginalTitle()); em.persist(work);
                    destination.setWork(work);
                    audit.logUpdate(AuditEntityType.ORIGINAL_BOOK, destination.getId(), userId, json("workId", null), json("workId", work.getId()));
                    touch(destination);
                }
                book.setWork(work);
            }
            audit.logUpdate(AuditEntityType.ORIGINAL_BOOK, id, userId, json("workId", previousWork), json("workId", book.getWork() == null ? null : book.getWork().getId()));
            // 번역서 자체는 바뀌지 않는다. 검색 이벤트만 발행해 잠그지 않은 자식 행을 UPDATE하지 않는다.
            book.getTranslations().forEach(this::publishUpdate);
        }
        touch(source);
    }

    private void validate(Media source, Media target, RelationshipRequest r) {
        if (source.getStatus() == ContentStatus.DELETED || target != null && target.getStatus() == ContentStatus.DELETED)
            fail("삭제된 도서는 관계를 수정할 수 없습니다.");
        if (r.action() == RelationshipRequest.Action.LEAVE_WORK) {
            if (!(source instanceof OriginalBook o) || o.getWork() == null) fail("작품에 연결된 원서만 분리할 수 있습니다.");
            if (target != null) fail("분리 작업에는 대상 도서를 지정하지 않습니다.");
            return;
        }
        if (!(target instanceof OriginalBook) || source.getId().equals(target.getId())) fail("서로 다른 원서를 대상으로 선택하세요.");
        if (r.action() == RelationshipRequest.Action.MOVE_TRANSLATION) {
            if (!(source instanceof TranslatedBook)) fail("번역서만 연결 원서를 변경할 수 있습니다.");
            TranslatedBook t = (TranslatedBook) source;
            if (t.getOriginalBook().getId().equals(target.getId())) fail("이미 연결된 원서입니다.");
            if (source.isPublic() && !target.isPublic()) fail("공개 번역서는 공개된 원서에 연결해야 합니다.");
        } else {
            if (!(source instanceof OriginalBook)) fail("원서끼리 같은 작품의 다른 판으로 연결할 수 있습니다.");
            OriginalBook s = (OriginalBook) source, t = (OriginalBook) target;
            if (s.getWork() != null && t.getWork() != null && s.getWork().getId().equals(t.getWork().getId())) fail("이미 같은 작품에 연결되어 있습니다.");
        }
    }
    private Media find(UUID id, boolean lock) {
        Object value = lock ? em.find(Media.class, id, LockModeType.PESSIMISTIC_WRITE) : em.find(Media.class, id);
        Media book = (Media) Hibernate.unproxy(value);
        if (!(book instanceof OriginalBook) && !(book instanceof TranslatedBook)) throw new ResourceNotFoundException("Book", id);
        // 앞선 관계 탐색으로 1차 캐시에 있던 도서도 잠금 획득 시점의 값으로 검사한다.
        if (lock) em.refresh(book);
        return book;
    }
    private void touch(Media book) {
        book.setUpdatedAt(Instant.now());
        publishUpdate(book);
    }
    private void publishUpdate(Media book) {
        events.publishContentEvent(new ContentEvent(book instanceof OriginalBook ? ContentEventType.ORIGINAL_BOOK_UPDATED : ContentEventType.TRANSLATED_BOOK_UPDATED,
                book.getId(), book.getCreatedBy() == null ? null : book.getCreatedBy().getId()));
    }
    private static String json(String key, UUID id) { return "{\"" + key + "\":" + (id == null ? "null" : "\"" + id + "\"") + "}"; }
    private static void fail(String text) { throw new ValidationException(text, Map.of()); }
    private static void requireAdmin() { if (!SecurityUtils.hasRole("ADMIN")) fail("관리자만 도서 관계를 수정할 수 있습니다."); }
}
