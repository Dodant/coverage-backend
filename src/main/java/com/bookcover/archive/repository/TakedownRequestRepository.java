package com.bookcover.archive.repository;

import com.bookcover.archive.domain.takedown.TakedownRequest;
import com.bookcover.archive.domain.takedown.TakedownStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * 저작권 삭제 요청 Repository.
 * 저작권 삭제 요청 엔티티에 대한 데이터 접근을 제공합니다.
 */
@Repository
public interface TakedownRequestRepository extends JpaRepository<TakedownRequest, UUID> {

    /**
     * 상태로 삭제 요청 목록을 조회합니다.
     *
     * @param status 삭제 요청 상태
     * @param pageable 페이징 정보
     * @return 삭제 요청 페이지
     */
    Page<TakedownRequest> findByStatus(TakedownStatus status, Pageable pageable);

    /**
     * 대상 ID로 삭제 요청 목록을 조회합니다.
     *
     * @param targetId 대상 ID
     * @return 삭제 요청 목록
     */
    List<TakedownRequest> findByTargetId(UUID targetId);

    /**
     * 요청자 이메일로 삭제 요청 목록을 조회합니다.
     *
     * @param requesterEmail 요청자 이메일
     * @param pageable 페이징 정보
     * @return 삭제 요청 페이지
     */
    Page<TakedownRequest> findByRequesterEmail(String requesterEmail, Pageable pageable);

    /**
     * 대기 중인 삭제 요청 목록을 생성일 기준 오름차순으로 조회합니다.
     *
     * @param status 삭제 요청 상태
     * @param pageable 페이징 정보
     * @return 삭제 요청 페이지
     */
    Page<TakedownRequest> findByStatusOrderByCreatedAtAsc(TakedownStatus status, Pageable pageable);

    /**
     * 상태별 삭제 요청 수를 조회합니다.
     *
     * @param status 삭제 요청 상태
     * @return 삭제 요청 수
     */
    long countByStatus(TakedownStatus status);

    /**
     * 대상 ID에 대해 승인된 삭제 요청이 있는지 확인합니다 (재업로드 방지).
     *
     * @param targetId 대상 ID
     * @param preventReupload 재업로드 방지 플래그
     * @return 존재 여부
     */
    boolean existsByTargetIdAndPreventReupload(UUID targetId, Boolean preventReupload);
}
