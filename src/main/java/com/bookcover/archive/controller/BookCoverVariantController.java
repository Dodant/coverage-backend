package com.bookcover.archive.controller;

import com.bookcover.archive.config.ratelimit.RateLimited;
import com.bookcover.archive.service.RateLimitAction;
import com.bookcover.archive.config.security.CustomUserDetails;
import com.bookcover.archive.controller.dto.book.BookCoverResponse;
import com.bookcover.archive.domain.book.BookCover;
import com.bookcover.archive.service.BookService;
import com.bookcover.archive.service.dto.AddCoverRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 같은 책(ISBN)의 앞면 표지 변형 API.
 * 이미지는 먼저 POST /covers 로 업로드하고, 받은 URL로 여기에 등록합니다.
 * 표지 목록은 원서/번역서 조회 응답의 covers 필드로 내려갑니다.
 */
@RestController
@RequestMapping("/books/{bookId}/covers")
@Tag(name = "Cover Variants", description = "책별 표지 변형(리커버, 쇄별 표지 등) API")
public class BookCoverVariantController {

    private final BookService bookService;

    public BookCoverVariantController(BookService bookService) {
        this.bookService = bookService;
    }

    @PostMapping
    @RateLimited(action = RateLimitAction.UPLOAD)
    @Operation(summary = "표지 변형 추가", description = "원서 또는 번역서에 앞면 표지를 추가합니다. 대표 표지는 바뀌지 않습니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "추가 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "404", description = "책 없음")
    })
    public ResponseEntity<BookCoverResponse> addCover(
            @PathVariable UUID bookId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AddCoverRequest request) {
        BookCover cover = bookService.addCover(bookId, request, userDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(BookCoverResponse.from(cover));
    }

    @PutMapping("/{coverId}/representative")
    @Operation(summary = "대표 표지 지정", description = "이 책의 표지 중 하나를 대표 표지(frontCover)로 지정합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "지정 성공"),
            @ApiResponse(responseCode = "400", description = "다른 책의 표지"),
            @ApiResponse(responseCode = "404", description = "책 또는 표지 없음")
    })
    public ResponseEntity<Void> setRepresentative(@PathVariable UUID bookId, @PathVariable UUID coverId) {
        bookService.setRepresentativeCover(bookId, coverId);
        return ResponseEntity.noContent().build();
    }
}
