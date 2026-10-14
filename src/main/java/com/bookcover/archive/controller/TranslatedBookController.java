package com.bookcover.archive.controller;

import com.bookcover.archive.config.ratelimit.RateLimited;
import com.bookcover.archive.service.RateLimitAction;
import com.bookcover.archive.config.security.CustomUserDetails;
import com.bookcover.archive.controller.dto.book.TranslatedBookResponse;
import com.bookcover.archive.domain.book.TranslatedBook;
import com.bookcover.archive.exception.ResourceNotFoundException;
import com.bookcover.archive.service.BookService;
import com.bookcover.archive.service.dto.CreateTranslatedBookRequest;
import com.bookcover.archive.service.dto.UpdateTranslatedBookRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 번역서 API 컨트롤러.
 * 번역서 CRUD 엔드포인트를 제공합니다.
 *
 * Requirements: 2.1, 2.6
 */
@RestController
@RequestMapping("/books/translated")
@Tag(name = "Translated Books", description = "번역서 관련 API")
public class TranslatedBookController {

    private final BookService bookService;

    public TranslatedBookController(BookService bookService) {
        this.bookService = bookService;
    }

    /**
     * 번역서를 등록합니다.
     *
     * Requirements: 2.1 - 번역서를 기존 원서와 연결
     */
    @PostMapping("/original/{originalBookId}")
    @RateLimited(action = RateLimitAction.UPLOAD)
    @Operation(summary = "번역서 등록", description = "원서에 연결된 새 번역서를 등록합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패)"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "원서 없음"),
            @ApiResponse(responseCode = "409", description = "ISBN 중복")
    })
    public ResponseEntity<TranslatedBookResponse> createTranslatedBook(
            @PathVariable UUID originalBookId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateTranslatedBookRequest request) {
        CreateTranslatedBookRequest requestWithUser = new CreateTranslatedBookRequest(
                request.title(),
                request.countryCode(),
                request.languageCode(),
                request.year(),
                request.isbn(),
                request.authorId(),
                request.authorName(),
                request.translatorId(),
                request.translatorName(),
                request.publisherId(),
                request.publisherName(),
                request.edition(),
                request.seriesId(),
                request.seriesName(),
                request.seriesNumber(),
                request.frontCover(),
                request.backCover(),
                userDetails.getId()
        );
        TranslatedBook book = bookService.createTranslatedBook(originalBookId, requestWithUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(TranslatedBookResponse.from(book));
    }

    /**
     * 번역서를 조회합니다.
     */
    @GetMapping("/{id}")
    @Operation(summary = "번역서 조회", description = "번역서 상세 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "번역서 없음")
    })
    public ResponseEntity<TranslatedBookResponse> getTranslatedBook(@PathVariable UUID id) {
        TranslatedBook book = bookService.getTranslatedBook(id)
                .orElseThrow(() -> new ResourceNotFoundException("TranslatedBook", id));
        return ResponseEntity.ok(TranslatedBookResponse.from(book));
    }

    /**
     * 번역서를 수정합니다.
     */
    @PutMapping("/{id}")
    @RateLimited(action = RateLimitAction.UPLOAD)
    @Operation(summary = "번역서 수정", description = "번역서 정보를 수정합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "번역서 없음")
    })
    public ResponseEntity<TranslatedBookResponse> updateTranslatedBook(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTranslatedBookRequest request) {
        TranslatedBook book = bookService.updateTranslatedBook(id, request);
        return ResponseEntity.ok(TranslatedBookResponse.from(book));
    }

    /**
     * 번역서를 삭제합니다.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "번역서 삭제", description = "번역서를 삭제합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "번역서 없음")
    })
    public ResponseEntity<Void> deleteTranslatedBook(@PathVariable UUID id) {
        bookService.deleteTranslatedBook(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 원서의 모든 번역서 목록을 조회합니다.
     *
     * Requirements: 2.6 - 원서에 여러 번역서가 등록되면 모든 번역서를 원서 상세 페이지에서 조회 가능
     */
    @GetMapping("/by-original/{originalBookId}")
    @Operation(summary = "원서별 번역서 조회", description = "특정 원서에 연결된 모든 번역서를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "원서 없음")
    })
    public ResponseEntity<List<TranslatedBookResponse>> getTranslationsByOriginalBook(
            @PathVariable UUID originalBookId) {
        bookService.getOriginalBook(originalBookId)
                .orElseThrow(() -> new ResourceNotFoundException("OriginalBook", originalBookId));

        List<TranslatedBook> translations = bookService.getTranslationsByOriginalBook(originalBookId);
        List<TranslatedBookResponse> response = translations.stream()
                .map(TranslatedBookResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }
}
