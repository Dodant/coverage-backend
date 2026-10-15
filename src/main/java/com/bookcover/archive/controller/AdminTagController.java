package com.bookcover.archive.controller;

import com.bookcover.archive.controller.dto.tag.TagResponse;
import com.bookcover.archive.domain.tag.TagCategory;
import com.bookcover.archive.service.TagService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 태그 사전과 커버 태그 API. Requirements: 20.1, 20.4. */
@RestController
@RequestMapping("/admin")
public class AdminTagController {
    private final TagService tagService;

    public AdminTagController(TagService tagService) { this.tagService = tagService; }

    public record CreateTagRequest(@NotBlank String name, @NotNull TagCategory category) {}

    @PostMapping("/tags")
    public ResponseEntity<TagResponse> create(@Valid @RequestBody CreateTagRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TagResponse.from(tagService.createTag(request.name(), request.category())));
    }

    @PutMapping("/covers/{coverId}/tags/{tagId}")
    public ResponseEntity<Void> assign(@PathVariable UUID coverId, @PathVariable UUID tagId) {
        tagService.assignTag(coverId, tagId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/covers/{coverId}/tags/{tagId}")
    public ResponseEntity<Void> remove(@PathVariable UUID coverId, @PathVariable UUID tagId) {
        tagService.removeTag(coverId, tagId);
        return ResponseEntity.noContent().build();
    }
}
