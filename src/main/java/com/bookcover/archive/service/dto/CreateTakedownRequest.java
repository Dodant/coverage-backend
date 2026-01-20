package com.bookcover.archive.service.dto;

import java.util.UUID;

/**
 * 저작권 삭제 요청 생성 DTO.
 *
 * @param requesterName 요청자 이름
 * @param requesterEmail 요청자 이메일
 * @param requesterOrganization 요청자 소속 (선택)
 * @param targetUrl 대상 콘텐츠 URL
 * @param targetId 대상 콘텐츠 ID (선택)
 * @param reason 삭제 요청 사유
 * @param evidenceUrl 증빙 자료 URL (선택)
 * @param evidenceDescription 증빙 자료 설명 (선택)
 */
public record CreateTakedownRequest(
        String requesterName,
        String requesterEmail,
        String requesterOrganization,
        String targetUrl,
        UUID targetId,
        String reason,
        String evidenceUrl,
        String evidenceDescription
) {}
