package com.bookcover.archive.controller;

import com.bookcover.archive.controller.dto.book.OriginalBookResponse;
import com.bookcover.archive.controller.dto.contributor.AuthorProfileResponse;
import com.bookcover.archive.controller.dto.contributor.AuthorResponse;
import com.bookcover.archive.domain.book.OriginalBook;
import com.bookcover.archive.domain.contributor.Author;
import com.bookcover.archive.exception.ResourceNotFoundException;
import com.bookcover.archive.repository.AuthorRepository;
import com.bookcover.archive.repository.OriginalBookRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 작가 API 컨트롤러.
 * 작가 프로필 및 작품 목록 조회 엔드포인트를 제공합니다.
 *
 * Requirements: 10.2, 10.4
 */
@RestController
@RequestMapping("/authors")
@Tag(name = "Authors", description = "작가 관련 API")
public class AuthorController {

    private final AuthorRepository authorRepository;
    private final OriginalBookRepository originalBookRepository;

    public AuthorController(AuthorRepository authorRepository, OriginalBookRepository originalBookRepository) {
        this.authorRepository = authorRepository;
        this.originalBookRepository = originalBookRepository;
    }

    /**
     * 작가 프로필을 조회합니다.
     *
     * Requirements: 10.4 - 작가 전용 프로필 페이지 제공
     * Requirements: 10.5 - 엔티티의 기본 정보와 관련 북커버 수 표시
     */
    @GetMapping("/{id}")
    @Operation(summary = "작가 프로필 조회", description = "작가의 프로필 정보와 관련 북커버 수를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "작가 없음")
    })
    public ResponseEntity<AuthorProfileResponse> getAuthorProfile(@PathVariable UUID id) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Author", id));
        long bookCount = originalBookRepository.countByAuthorId(id);
        return ResponseEntity.ok(AuthorProfileResponse.from(author, bookCount));
    }

    /**
     * 작가의 작품 목록을 조회합니다.
     *
     * Requirements: 10.2 - 작가가 쓴 모든 책의 북커버 목록 표시
     */
    @GetMapping("/{id}/books")
    @Operation(summary = "작가 작품 목록 조회", description = "작가가 쓴 모든 책의 북커버 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "작가 없음")
    })
    public ResponseEntity<Page<OriginalBookResponse>> getAuthorBooks(
            @PathVariable UUID id,
            @PageableDefault(size = 20) Pageable pageable) {
        authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Author", id));
        Page<OriginalBook> books = originalBookRepository.findByAuthorId(id, pageable);
        Page<OriginalBookResponse> response = books.map(OriginalBookResponse::from);
        return ResponseEntity.ok(response);
    }

    /**
     * 작가 목록을 조회합니다.
     */
    @GetMapping
    @Operation(summary = "작가 목록 조회", description = "작가 목록을 페이징하여 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<Page<AuthorResponse>> getAuthors(
            @RequestParam(required = false) String name,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Author> authors;
        if (name != null && !name.isBlank()) {
            authors = authorRepository.findByNameContainingIgnoreCase(name, pageable);
        } else {
            authors = authorRepository.findAll(pageable);
        }
        Page<AuthorResponse> response = authors.map(AuthorResponse::from);
        return ResponseEntity.ok(response);
    }
}
