package com.bookcover.archive.service.impl;

import com.bookcover.archive.domain.book.OriginalBook;
import com.bookcover.archive.domain.book.Redesign;
import com.bookcover.archive.domain.book.TranslatedBook;
import com.bookcover.archive.domain.common.ContentStatus;
import com.bookcover.archive.domain.user.User;
import com.bookcover.archive.event.ContentEvent;
import com.bookcover.archive.event.ContentEventType;
import com.bookcover.archive.event.EventPublisher;
import com.bookcover.archive.exception.ResourceNotFoundException;
import com.bookcover.archive.exception.ValidationException;
import com.bookcover.archive.repository.OriginalBookRepository;
import com.bookcover.archive.repository.RedesignRepository;
import com.bookcover.archive.repository.TranslatedBookRepository;
import com.bookcover.archive.repository.UserRepository;
import com.bookcover.archive.service.ContributionService;
import com.bookcover.archive.service.RedesignService;
import com.bookcover.archive.service.dto.CreateRedesignRequest;
import com.bookcover.archive.service.dto.UpdateRedesignRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 리디자인 관리 서비스 구현체.
 *
 * Requirements:
 * - 3.1: 리디자인을 업로드하면 해당 리디자인을 원서 또는 번역서와 연결
 * - 3.2: 리디자인 이미지, 설명을 필수 항목으로 검증
 * - 3.5: 한 책에 대해 여러 회원이 각각 리디자인을 업로드하는 것을 허용
 * - 3.6: 한 회원이 같은 책에 대해 여러 개의 리디자인을 업로드하는 것을 허용
 */
@Service
@Transactional
public class RedesignServiceImpl implements RedesignService {

    private final RedesignRepository redesignRepository;
    private final OriginalBookRepository originalBookRepository;
    private final TranslatedBookRepository translatedBookRepository;
    private final UserRepository userRepository;
    private final ContributionService contributionService;
    private final EventPublisher eventPublisher;

    public RedesignServiceImpl(
            RedesignRepository redesignRepository,
            OriginalBookRepository originalBookRepository,
            TranslatedBookRepository translatedBookRepository,
            UserRepository userRepository,
            ContributionService contributionService,
            EventPublisher eventPublisher) {
        this.redesignRepository = redesignRepository;
        this.originalBookRepository = originalBookRepository;
        this.translatedBookRepository = translatedBookRepository;
        this.userRepository = userRepository;
        this.contributionService = contributionService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Redesign createRedesign(CreateRedesignRequest request) {
        // 원서 또는 번역서 중 하나는 반드시 지정되어야 함
        if (!request.hasValidBookReference()) {
            throw new ValidationException(
                "원서 또는 번역서 중 하나는 반드시 지정되어야 합니다",
                Map.of("originalBookId", "null", "translatedBookId", "null")
            );
        }

        Redesign redesign = new Redesign();
        redesign.setImageUrl(request.imageUrl());
        redesign.setThumbnailUrl(request.thumbnailUrl());
        redesign.setDescription(request.description());
        redesign.setStatus(ContentStatus.PUBLISHED);

        // 원서 연결
        if (request.originalBookId() != null) {
            OriginalBook originalBook = originalBookRepository.findById(request.originalBookId())
                    .orElseThrow(() -> new ResourceNotFoundException("OriginalBook", request.originalBookId()));
            redesign.setOriginalBook(originalBook);
        }

        // 번역서 연결
        if (request.translatedBookId() != null) {
            TranslatedBook translatedBook = translatedBookRepository.findById(request.translatedBookId())
                    .orElseThrow(() -> new ResourceNotFoundException("TranslatedBook", request.translatedBookId()));
            redesign.setTranslatedBook(translatedBook);
        }

        // 등록자 연결
        User user = userRepository.findById(request.createdById())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.createdById()));
        redesign.setCreatedBy(user);

        Redesign savedRedesign = redesignRepository.save(redesign);

        // 기여 횟수 증가 및 칭호 업데이트
        contributionService.incrementContribution(request.createdById());

        // 콘텐츠 생성 이벤트 발행
        eventPublisher.publishContentEvent(new ContentEvent(
                ContentEventType.REDESIGN_CREATED,
                savedRedesign.getId(),
                request.createdById()
        ));

        return savedRedesign;
    }

    @Override
    public Redesign updateRedesign(UUID id, UpdateRedesignRequest request) {
        Redesign redesign = redesignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Redesign", id));

        if (request.imageUrl() != null) {
            redesign.setImageUrl(request.imageUrl());
        }
        if (request.thumbnailUrl() != null) {
            redesign.setThumbnailUrl(request.thumbnailUrl());
        }
        if (request.description() != null) {
            redesign.setDescription(request.description());
        }

        redesign.setUpdatedAt(Instant.now());
        Redesign savedRedesign = redesignRepository.save(redesign);

        // 콘텐츠 수정 이벤트 발행
        UUID userId = redesign.getCreatedBy() != null ? redesign.getCreatedBy().getId() : null;
        eventPublisher.publishContentEvent(new ContentEvent(
                ContentEventType.REDESIGN_UPDATED,
                savedRedesign.getId(),
                userId
        ));

        return savedRedesign;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Redesign> getRedesign(UUID id) {
        return redesignRepository.findById(id);
    }

    @Override
    public void deleteRedesign(UUID id) {
        Redesign redesign = redesignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Redesign", id));
        redesign.setStatus(ContentStatus.DELETED);
        redesign.setUpdatedAt(Instant.now());
        redesignRepository.save(redesign);

        // 콘텐츠 삭제 이벤트 발행
        UUID userId = redesign.getCreatedBy() != null ? redesign.getCreatedBy().getId() : null;
        eventPublisher.publishContentEvent(new ContentEvent(
                ContentEventType.REDESIGN_DELETED,
                id,
                userId
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Redesign> getRedesignsByOriginalBook(UUID originalBookId, Pageable pageable) {
        return redesignRepository.findByOriginalBookId(originalBookId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Redesign> getRedesignsByTranslatedBook(UUID translatedBookId, Pageable pageable) {
        return redesignRepository.findByTranslatedBookId(translatedBookId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Redesign> getRedesignsByUser(UUID userId, Pageable pageable) {
        return redesignRepository.findByCreatedById(userId, pageable);
    }
}
