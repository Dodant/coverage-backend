package com.bookcover.archive.controller.dto.admin;

/**
 * 저작권 삭제 요청 검토 요청 DTO.
 */
public record ReviewTakedownRequest(
        String note
) {
}
