package com.bookcover.archive.controller.admin;

import com.bookcover.archive.config.security.CustomUserDetails;
import com.bookcover.archive.controller.dto.book.BookCoverResponse;
import com.bookcover.archive.domain.common.ContentStatus;
import com.bookcover.archive.service.CatalogService;
import com.bookcover.archive.service.dto.UpdateCoverMetadataRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import java.util.List;

/** 전체 도서·보완 작업 목록과 표지 버전 관리. 기존 초안 원서 API는 유지한다. */
@RestController
@RequestMapping("/admin/catalog")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCatalogController {
    private final CatalogService catalog;
    public AdminCatalogController(CatalogService catalog) { this.catalog = catalog; }

    @GetMapping
    public Page<CatalogService.Item> list(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(required = false) CatalogService.Kind kind,
            @RequestParam(required = false) ContentStatus status,
            @RequestParam(defaultValue = "") String language,
            @RequestParam(defaultValue = "") String publisher,
            @RequestParam(required = false) Boolean hasCover,
            @RequestParam(required = false) CatalogService.Issue issue,
            @RequestParam(defaultValue = "false") boolean needsAttention,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            @RequestParam(defaultValue = "created") String sort) {
        return catalog.list(new CatalogService.Filter(query, kind, status, language, publisher, hasCover, issue, needsAttention), page, size, sort);
    }

    @GetMapping("/{id}")
    public CatalogService.Detail detail(@PathVariable UUID id) { return catalog.detail(id); }

    @GetMapping("/{id}/editions")
    public List<Object> editions(@PathVariable UUID id) { return catalog.editions(id); }

    @PutMapping("/{bookId}/covers/{coverId}")
    public BookCoverResponse updateCover(@PathVariable UUID bookId, @PathVariable UUID coverId,
            @Valid @RequestBody UpdateCoverMetadataRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        return catalog.updateCover(bookId, coverId, request, user.getId());
    }
}
