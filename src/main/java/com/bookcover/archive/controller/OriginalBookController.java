package com.bookcover.archive.controller;

import com.bookcover.archive.config.ratelimit.RateLimited;
import com.bookcover.archive.service.RateLimitAction;
import com.bookcover.archive.config.security.CustomUserDetails;
import com.bookcover.archive.controller.dto.book.OriginalBookResponse;
import com.bookcover.archive.controller.dto.book.TranslatedBookResponse;
import com.bookcover.archive.domain.book.OriginalBook;
import com.bookcover.archive.domain.book.TranslatedBook;
import com.bookcover.archive.domain.media.Media;
import com.bookcover.archive.exception.ResourceNotFoundException;
import com.bookcover.archive.service.BookService;
import com.bookcover.archive.service.dto.CreateOriginalBookRequest;
import com.bookcover.archive.service.dto.UpdateOriginalBookRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 원서 API 컨트롤러.
 * 원서 CRUD 엔드포인트를 제공합니다.
 *
 * Requirements: 1.1, 1.5
 */
@RestController
@RequestMapping("/books/original")
@Tag(name = "Original Books", description = "원서 관련 API")
public class OriginalBookController {

    private final BookService bookService;

    public OriginalBookController(BookService bookService) {
        this.bookService = bookService;
    }

    /**
     * 원서를 등록합니다.
     *
     * Requirements: 1.1 - 필수 항목 검증, 1.5 - 고유 식별자 생성 및 저장
     */
    @PostMapping
    @RateLimited(action = RateLimitAction.UPLOAD)
    @Operation(summary = "원서 등록", description = "새 원서를 등록합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패)"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "409", description = "ISBN 중복")
    })
    public ResponseEntity<OriginalBookResponse> createOriginalBook(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateOriginalBookRequest request) {
        OriginalBook book = bookService.createOriginalBook(request.withCreatedBy(userDetails.getId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(OriginalBookResponse.from(book));
    }

    /**
     * 원서를 조회합니다.
     */
    @GetMapping("/{id}")
    @Operation(summary = "원서 조회", description = "원서 상세 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "원서 없음")
    })
    public ResponseEntity<OriginalBookResponse> getOriginalBook(@PathVariable UUID id) {
        OriginalBook book = bookService.getOriginalBook(id)
                .orElseThrow(() -> new ResourceNotFoundException("OriginalBook", id));
        return ResponseEntity.ok(OriginalBookResponse.from(book));
    }


    /**
     * 원서를 수정합니다.
     */
    @PutMapping("/{id}")
    @RateLimited(action = RateLimitAction.UPLOAD)
    @Operation(summary = "원서 수정", description = "원서 정보를 수정합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "원서 없음")
    })
    public ResponseEntity<OriginalBookResponse> updateOriginalBook(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOriginalBookRequest request) {
        OriginalBook book = bookService.updateOriginalBook(id, request, userDetails.getId());
        return ResponseEntity.ok(OriginalBookResponse.from(book));
    }

    /**
     * 원서를 삭제합니다.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "원서 삭제", description = "원서를 삭제합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "원서 없음")
    })
    public ResponseEntity<Void> deleteOriginalBook(@PathVariable UUID id) {
        bookService.deleteOriginalBook(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 원서의 번역서 목록을 조회합니다.
     */
    @GetMapping("/{id}/translations")
    @Operation(summary = "번역서 목록 조회", description = "원서에 연결된 모든 번역서 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "원서 없음")
    })
    public ResponseEntity<List<TranslatedBookResponse>> getTranslations(@PathVariable UUID id) {
        bookService.getOriginalBook(id)
                .orElseThrow(() -> new ResourceNotFoundException("OriginalBook", id));

        List<TranslatedBook> translations = bookService.getTranslationsByOriginalBook(id);
        List<TranslatedBookResponse> response = translations.stream()
                .map(TranslatedBookResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * ISBN으로 중복 검사를 수행합니다.
     */
    @GetMapping("/check-duplicate")
    @Operation(summary = "ISBN 중복 검사", description = "ISBN으로 중복 여부를 확인합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검사 완료")
    })
    public ResponseEntity<List<DuplicateCheckResponse>> checkDuplicate(@RequestParam String isbn) {
        List<Media> duplicates = bookService.findDuplicatesByIsbn(isbn);
        List<DuplicateCheckResponse> response = duplicates.stream()
                .map(media -> new DuplicateCheckResponse(media.getId(), media.getTitle(), media.getClass().getSimpleName()))
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * 유사한 책을 검색합니다.
     */
    @GetMapping("/find-similar")
    @Operation(summary = "유사 책 검색", description = "제목, 저자, 연도(선택, ±2년)로 유사한 책을 검색합니다. 연도를 모르는 책도 포함합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검색 완료")
    })
    public ResponseEntity<List<DuplicateCheckResponse>> findSimilar(
            @RequestParam String title,
            @RequestParam String authorName,
            @RequestParam(required = false) Integer year) {
        List<Media> similar = bookService.findSimilarBooks(title, authorName, year);
        List<DuplicateCheckResponse> response = similar.stream()
                .map(media -> new DuplicateCheckResponse(media.getId(), media.getTitle(), media.getClass().getSimpleName()))
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * 작가별 원서 목록을 조회합니다.
     */
    @GetMapping("/by-author/{authorId}")
    @Operation(summary = "작가별 원서 조회", description = "특정 작가의 모든 원서를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<Page<OriginalBookResponse>> getBooksByAuthor(
            @PathVariable UUID authorId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<OriginalBook> books = bookService.getBooksByAuthor(authorId, pageable);
        Page<OriginalBookResponse> response = books.map(OriginalBookResponse::from);
        return ResponseEntity.ok(response);
    }

    /**
     * 출판사별 원서 목록을 조회합니다.
     */
    @GetMapping("/by-publisher/{publisherId}")
    @Operation(summary = "출판사별 원서 조회", description = "특정 출판사의 모든 원서를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<Page<OriginalBookResponse>> getBooksByPublisher(
            @PathVariable UUID publisherId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<OriginalBook> books = bookService.getBooksByPublisher(publisherId, pageable);
        Page<OriginalBookResponse> response = books.map(OriginalBookResponse::from);
        return ResponseEntity.ok(response);
    }

    /**
     * 중복 검사 응답 DTO.
     */
    public record DuplicateCheckResponse(UUID id, String title, String type) {}
}
