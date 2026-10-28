package com.bookcover.archive.controller.admin;

import com.bookcover.archive.config.security.CustomUserDetails;
import com.bookcover.archive.service.RelationshipService;
import com.bookcover.archive.service.dto.RelationshipRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

/** 도서 관계 변경의 검토와 적용을 별도 요청으로 처리한다. */
@RestController
@RequestMapping("/admin/catalog/{id}")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRelationshipController {
    private final RelationshipService service;
    public AdminRelationshipController(RelationshipService service) { this.service = service; }
    @PostMapping("/relationship-preview")
    public RelationshipService.Preview preview(@PathVariable UUID id, @Valid @RequestBody RelationshipRequest request) { return service.preview(id, request); }
    @PutMapping("/relationship")
    public ResponseEntity<Void> apply(@PathVariable UUID id, @Valid @RequestBody RelationshipRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        service.apply(id, request, user.getId()); return ResponseEntity.noContent().build();
    }
}
