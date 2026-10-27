package com.bookcover.archive.controller.admin;

import com.bookcover.archive.controller.dto.book.OriginalBookResponse;
import com.bookcover.archive.domain.common.ContentStatus;
import com.bookcover.archive.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 원서 목록 API. 공개 목록에 안 나오는 상태(DRAFT 등)의 책을 백오피스 '퍼블리시'에서 검토한다.
 * 공개 전환은 POST /admin/moderation/content/restore(PUBLISHED로 바꾸고 재색인)를 쓴다.
 *
 * Requirements: 15.1 - 등록 즉시 공개의 예외로, 관리자가 DRAFT로 넣고 검토 뒤 공개한다
 */
@RestController
@RequestMapping("/admin/books")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Books", description = "관리자 원서 관리 API")
@SecurityRequirement(name = "bearerAuth")
public class AdminBookController {

    private final BookService bookService;

    public AdminBookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    @Operation(summary = "상태별 원서 목록", description = "status 상태의 원서를 최근 등록 순으로 돌려줍니다(기본 DRAFT).")
    public ResponseEntity<Page<OriginalBookResponse>> list(
            @RequestParam(defaultValue = "DRAFT") ContentStatus status,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(bookService.getBooksByStatus(status, pageable).map(OriginalBookResponse::from));
    }
}
