package com.bookcover.archive.service;

import com.bookcover.archive.domain.takedown.TakedownRequest;
import com.bookcover.archive.domain.takedown.TakedownStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * 저작권 삭제 요청 서비스 인터페이스.
 * 권리자(출판사/대리인)의 저작권 삭제 요청을 처리합니다.
 *
 * Requirements: 17.1, 17.3, 17.4
 */
public interface TakedownService {

    /**
     * 저작권 삭제 요청을 접수합니다.
     *
     * @param requesterName 요청자 이름
     * @param requesterEmail 요청자 이메일
     * @param requesterOrganization 요청자 소속 (선택)
     * @param targetUrl 대상 콘텐츠 URL
     * @param targetId 대상 콘텐츠 ID (선택)
     * @param reason 삭제 요청 사유
     * @param evidenceUrl 증빙 자료 URL (선택)
     * @param evidenceDescription 증빙 자료 설명 (선택)
     * @return 생성된 삭제 요청
     */
    TakedownRequest createRequest(String requesterName, String requesterEmail, String requesterOrganization,
                                  String targetUrl, UUID targetId, String reason,
                                  String evidenceUrl, String evidenceDescription);

    /**
     * 삭제 요청을 조회합니다.
     *
     * @param requestId 요청 ID
     * @return 삭제 요청 Optional
     */
    Optional<TakedownRequest> getRequest(UUID requestId);

    /**
     * 대기 중인 삭제 요청 큐를 조회합니다.
     *
     * @param pageable 페이징 정보
     * @return 삭제 요청 페이지
     */
    Page<TakedownRequest> getPendingRequests(Pageable pageable);

    /**
     * 상태별 삭제 요청 목록을 조회합니다.
     *
     * @param status 삭제 요청 상태
     * @param pageable 페이징 정보
     * @return 삭제 요청 페이지
     */
    Page<TakedownRequest> getRequestsByStatus(TakedownStatus status, Pageable pageable);

    /**
     * 요청자 이메일로 삭제 요청 목록을 조회합니다.
     *
     * @param requesterEmail 요청자 이메일
     * @param pageable 페이징 정보
     * @return 삭제 요청 페이지
     */
    Page<TakedownRequest> getRequestsByRequester(String requesterEmail, Pageable pageable);

    /**
     * 삭제 요청을 승인합니다.
     * 해당 이미지를 비공개 처리하고 재업로드 방지 플래그를 설정합니다.
     *
     * @param requestId 요청 ID
     * @param reviewerId 검토자 ID
     * @param note 검토 메모
     * @return 처리된 삭제 요청
     */
    TakedownRequest approveRequest(UUID requestId, UUID reviewerId, String note);

    /**
     * 삭제 요청을 거부합니다.
     * 거부 사유를 요청자에게 통지합니다.
     *
     * @param requestId 요청 ID
     * @param reviewerId 검토자 ID
     * @param note 거부 사유
     * @return 처리된 삭제 요청
     */
    TakedownRequest rejectRequest(UUID requestId, UUID reviewerId, String note);

    /**
     * 특정 콘텐츠에 대해 재업로드가 방지되어 있는지 확인합니다.
     *
     * @param targetId 대상 콘텐츠 ID
     * @return 재업로드 방지 여부
     */
    boolean isReuploadPrevented(UUID targetId);

    /**
     * 상태별 삭제 요청 수를 조회합니다.
     *
     * @param status 삭제 요청 상태
     * @return 삭제 요청 수
     */
    long countByStatus(TakedownStatus status);
}
