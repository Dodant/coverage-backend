package com.bookcover.archive.service.impl;

import com.bookcover.archive.domain.book.BookCover;
import com.bookcover.archive.domain.book.OriginalBook;
import com.bookcover.archive.domain.book.Redesign;
import com.bookcover.archive.domain.book.TranslatedBook;
import com.bookcover.archive.domain.common.ContentStatus;
import com.bookcover.archive.domain.media.Media;
import com.bookcover.archive.domain.takedown.TakedownRequest;
import com.bookcover.archive.domain.takedown.TakedownStatus;
import com.bookcover.archive.domain.user.User;
import com.bookcover.archive.event.ContentEvent;
import com.bookcover.archive.event.ContentEventType;
import com.bookcover.archive.event.EventPublisher;
import com.bookcover.archive.exception.ResourceNotFoundException;
import com.bookcover.archive.exception.ValidationException;
import com.bookcover.archive.repository.BookCoverRepository;
import com.bookcover.archive.repository.OriginalBookRepository;
import com.bookcover.archive.repository.RedesignRepository;
import com.bookcover.archive.repository.TakedownRequestRepository;
import com.bookcover.archive.repository.TranslatedBookRepository;
import com.bookcover.archive.repository.UserRepository;
import com.bookcover.archive.service.TakedownService;
import com.bookcover.archive.service.ImageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 저작권 삭제 요청 서비스 구현체.
 * 권리자(출판사/대리인)의 저작권 삭제 요청을 처리합니다.
 *
 * Requirements: 17.1, 17.2, 17.3, 17.4
 */
@Service
@Transactional
public class TakedownServiceImpl implements TakedownService {

    private final TakedownRequestRepository takedownRequestRepository;
    private final UserRepository userRepository;
    private final OriginalBookRepository originalBookRepository;
    private final TranslatedBookRepository translatedBookRepository;
    private final RedesignRepository redesignRepository;
    private final BookCoverRepository bookCoverRepository;
    private final ImageService imageService;
    private final EventPublisher eventPublisher;

    public TakedownServiceImpl(TakedownRequestRepository takedownRequestRepository, UserRepository userRepository,
                               OriginalBookRepository originalBookRepository,
                               TranslatedBookRepository translatedBookRepository,
                               RedesignRepository redesignRepository, BookCoverRepository bookCoverRepository,
                               ImageService imageService, EventPublisher eventPublisher) {
        this.takedownRequestRepository = takedownRequestRepository;
        this.userRepository = userRepository;
        this.originalBookRepository = originalBookRepository;
        this.translatedBookRepository = translatedBookRepository;
        this.redesignRepository = redesignRepository;
        this.bookCoverRepository = bookCoverRepository;
        this.imageService = imageService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public TakedownRequest createRequest(String requesterName, String requesterEmail, String requesterOrganization,
                                         String targetUrl, UUID targetId, String reason,
                                         String evidenceUrl, String evidenceDescription) {
        // 필수 필드 검증
        if (requesterName == null || requesterName.isBlank()) {
            throw new ValidationException("Requester name is required", Map.of("requesterName", "null or blank"));
        }
        if (requesterEmail == null || requesterEmail.isBlank()) {
            throw new ValidationException("Requester email is required", Map.of("requesterEmail", "null or blank"));
        }
        if (targetUrl == null || targetUrl.isBlank()) {
            throw new ValidationException("Target URL is required", Map.of("targetUrl", "null or blank"));
        }
        if (reason == null || reason.isBlank()) {
            throw new ValidationException("Reason is required", Map.of("reason", "null or blank"));
        }

        TakedownRequest request = new TakedownRequest();
        request.setRequesterName(requesterName);
        request.setRequesterEmail(requesterEmail);
        request.setRequesterOrganization(requesterOrganization);
        request.setTargetUrl(targetUrl);
        request.setTargetId(targetId != null ? targetId : idFromUrl(targetUrl));
        request.setReason(reason);
        request.setEvidenceUrl(evidenceUrl);
        request.setEvidenceDescription(evidenceDescription);
        request.setStatus(TakedownStatus.PENDING);
        request.setPreventReupload(false);
        request.setCreatedAt(Instant.now());

        return takedownRequestRepository.save(request);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TakedownRequest> getRequest(UUID requestId) {
        return takedownRequestRepository.findById(requestId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TakedownRequest> getPendingRequests(Pageable pageable) {
        return takedownRequestRepository.findByStatusOrderByCreatedAtAsc(TakedownStatus.PENDING, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TakedownRequest> getRequestsByStatus(TakedownStatus status, Pageable pageable) {
        return takedownRequestRepository.findByStatus(status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TakedownRequest> getRequestsByRequester(String requesterEmail, Pageable pageable) {
        return takedownRequestRepository.findByRequesterEmail(requesterEmail, pageable);
    }

    @Override
    public TakedownRequest approveRequest(UUID requestId, UUID reviewerId, String note) {
        TakedownRequest request = takedownRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("TakedownRequest", requestId));

        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", reviewerId));

        // 문제 9 해결: 리뷰어가 ADMIN인지 확인
        if (reviewer.getRole() != com.bookcover.archive.domain.user.UserRole.ADMIN) {
            throw new ValidationException("Only administrators can approve takedown requests",
                    Map.of("userId", reviewerId, "role", reviewer.getRole().name()));
        }

        // 이미 처리된 요청인지 확인
        if (request.getStatus() != TakedownStatus.PENDING) {
            throw new ValidationException("Request has already been processed",
                    Map.of("requestId", requestId, "currentStatus", request.getStatus()));
        }

        UUID targetId = request.getTargetId();
        if (targetId == null) {
            targetId = bookCoverRepository.findByImageUrl(request.getTargetUrl()).stream()
                    .findFirst().map(BookCover::getId)
                    .or(() -> bookCoverRepository.findByThumbnailUrl(request.getTargetUrl()).stream()
                            .findFirst().map(BookCover::getId))
                    .or(() -> redesignRepository.findByImageUrl(request.getTargetUrl()).map(Redesign::getId))
                    .or(() -> redesignRepository.findByThumbnailUrl(request.getTargetUrl()).map(Redesign::getId))
                    .orElse(null);
        }
        hideTarget(targetId);
        request.setTargetId(targetId);
        request.approve(reviewer, note);

        return takedownRequestRepository.save(request);
    }

    @Override
    public TakedownRequest rejectRequest(UUID requestId, UUID reviewerId, String note) {
        TakedownRequest request = takedownRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("TakedownRequest", requestId));

        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", reviewerId));

        // 문제 9 해결: 리뷰어가 ADMIN인지 확인
        if (reviewer.getRole() != com.bookcover.archive.domain.user.UserRole.ADMIN) {
            throw new ValidationException("Only administrators can reject takedown requests",
                    Map.of("userId", reviewerId, "role", reviewer.getRole().name()));
        }

        // 이미 처리된 요청인지 확인
        if (request.getStatus() != TakedownStatus.PENDING) {
            throw new ValidationException("Request has already been processed",
                    Map.of("requestId", requestId, "currentStatus", request.getStatus()));
        }

        request.reject(reviewer, note);

        return takedownRequestRepository.save(request);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isReuploadPrevented(UUID targetId) {
        return takedownRequestRepository.existsByTargetIdAndPreventReupload(targetId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(TakedownStatus status) {
        return takedownRequestRepository.countByStatus(status);
    }

    private void hideTarget(UUID targetId) {
        if (targetId == null) {
            throw new ValidationException("삭제할 콘텐츠를 식별할 수 없습니다", Map.of("targetId", "필수입니다"));
        }
        Optional<OriginalBook> original = originalBookRepository.findById(targetId);
        if (original.isPresent()) {
            hideBook(original.get());
            return;
        }
        Optional<TranslatedBook> translated = translatedBookRepository.findById(targetId);
        if (translated.isPresent()) {
            hideBook(translated.get());
            return;
        }
        Optional<Redesign> redesign = redesignRepository.findById(targetId);
        if (redesign.isPresent()) {
            Redesign item = redesign.get();
            imageService.blockImage(item.getImageUrl(), item.getThumbnailUrl());
            item.setStatus(ContentStatus.HIDDEN);
            eventPublisher.publishContentEvent(new ContentEvent(ContentEventType.REDESIGN_UPDATED, item.getId(), null));
            return;
        }
        BookCover cover = bookCoverRepository.findById(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("Content", targetId));
        hideCover(cover);
        Media book = cover.getBook();
        if (book == null) {
            book = originalBookRepository.findByBackCoverId(targetId).<Media>map(value -> value)
                    .or(() -> translatedBookRepository.findByBackCoverId(targetId))
                    .orElseThrow(() -> new ResourceNotFoundException("BookForCover", targetId));
        }
        hideBookStatus(book);
    }

    private void hideBook(Media book) {
        Set<BookCover> covers = new LinkedHashSet<>(book.getCovers());
        if (book.getFrontCover() != null) covers.add(book.getFrontCover());
        if (book instanceof OriginalBook original && original.getBackCover() != null) {
            covers.add(original.getBackCover());
        } else if (book instanceof TranslatedBook translated && translated.getBackCover() != null) {
            covers.add(translated.getBackCover());
        }
        covers.forEach(this::hideCover);
        hideBookStatus(book);
    }

    private void hideCover(BookCover cover) {
        if (!cover.isHidden()) {
            imageService.blockImage(cover.getImageUrl(), cover.getThumbnailUrl());
            cover.setHidden(true);
        }
    }

    private void hideBookStatus(Media book) {
        book.setStatus(ContentStatus.HIDDEN);
        book.setUpdatedAt(Instant.now());
        ContentEventType type = book instanceof OriginalBook
                ? ContentEventType.ORIGINAL_BOOK_UPDATED : ContentEventType.TRANSLATED_BOOK_UPDATED;
        eventPublisher.publishContentEvent(new ContentEvent(type, book.getId(), null));
    }

    private static UUID idFromUrl(String value) {
        try {
            String path = URI.create(value).getPath();
            String last = path.substring(path.lastIndexOf('/') + 1);
            return UUID.fromString(last);
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
