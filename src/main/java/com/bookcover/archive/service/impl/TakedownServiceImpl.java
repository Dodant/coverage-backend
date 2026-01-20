package com.bookcover.archive.service.impl;

import com.bookcover.archive.domain.takedown.TakedownRequest;
import com.bookcover.archive.domain.takedown.TakedownStatus;
import com.bookcover.archive.domain.user.User;
import com.bookcover.archive.exception.ResourceNotFoundException;
import com.bookcover.archive.exception.ValidationException;
import com.bookcover.archive.repository.TakedownRequestRepository;
import com.bookcover.archive.repository.UserRepository;
import com.bookcover.archive.service.TakedownService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
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

    public TakedownServiceImpl(TakedownRequestRepository takedownRequestRepository, UserRepository userRepository) {
        this.takedownRequestRepository = takedownRequestRepository;
        this.userRepository = userRepository;
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
        request.setTargetId(targetId);
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
}
