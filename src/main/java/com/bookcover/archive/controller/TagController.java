package com.bookcover.archive.controller;

import com.bookcover.archive.controller.dto.tag.TagResponse;
import com.bookcover.archive.domain.tag.DesignTag;
import com.bookcover.archive.domain.tag.TagCategory;
import com.bookcover.archive.exception.ResourceNotFoundException;
import com.bookcover.archive.repository.DesignTagRepository;
import com.bookcover.archive.service.BrowsingService;
import com.bookcover.archive.service.dto.BrowseResult;
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

import java.util.List;
import java.util.UUID;

/**
 * 태그 API 컨트롤러.
 * 태그 목록 및 태그별 브라우징 엔드포인트를 제공합니다.
 *
 * Requirements: 20.3, 20.6
 */
@RestController
@RequestMapping("/tags")
@Tag(name = "Tags", description = "디자인 태그 관련 API")
public class TagController {

    private final DesignTagRepository designTagRepository;
    private final BrowsingService browsingService;

    public TagController(DesignTagRepository designTagRepository, BrowsingService browsingService) {
        this.designTagRepository = designTagRepository;
        this.browsingService = browsingService;
    }

    /**
     * 태그 목록을 조회합니다.
     *
     * Requirements: 20.3 - 태그/속성 필터 적용
     */
    @GetMapping
    @Operation(summary = "태그 목록 조회", description = "승인된 태그 목록을 페이징하여 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<Page<TagResponse>> getTags(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) TagCategory category,
            @PageableDefault(size = 50) Pageable pageable) {
        Page<DesignTag> tags;
        if (name != null && !name.isBlank()) {
            tags = designTagRepository.findByNameContainingIgnoreCaseAndApproved(name, true, pageable);
        } else if (category != null) {
            tags = designTagRepository.findByCategory(category, pageable);
        } else {
            tags = designTagRepository.findByApproved(true, pageable);
        }
        Page<TagResponse> response = tags.map(TagResponse::from);
        return ResponseEntity.ok(response);
    }

    /**
     * 인기 태그 목록을 조회합니다.
     */
    @GetMapping("/popular")
    @Operation(summary = "인기 태그 목록 조회", description = "사용 횟수 기준 인기 태그 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<List<BrowsingService.TagInfo>> getPopularTags(
            @RequestParam(defaultValue = "20") int limit) {
        List<BrowsingService.TagInfo> tags = browsingService.getPopularTags(limit);
        return ResponseEntity.ok(tags);
    }

    /**
     * 태그 상세 정보를 조회합니다.
     */
    @GetMapping("/{id}")
    @Operation(summary = "태그 상세 조회", description = "태그의 상세 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "태그 없음")
    })
    public ResponseEntity<TagResponse> getTag(@PathVariable UUID id) {
        DesignTag tag = designTagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DesignTag", id));
        return ResponseEntity.ok(TagResponse.from(tag));
    }

    /**
     * 태그별 북커버 브라우징을 수행합니다.
     *
     * Requirements: 20.6 - 태그별 브라우징 페이지 제공
     */
    @GetMapping("/{id}/covers")
    @Operation(summary = "태그별 북커버 브라우징", description = "특정 태그가 적용된 북커버 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "태그 없음")
    })
    public ResponseEntity<BrowseResult> browseByTag(
            @PathVariable UUID id,
            @PageableDefault(size = 20) Pageable pageable) {
        designTagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DesignTag", id));
        BrowseResult result = browsingService.browseByTag(id, pageable);
        return ResponseEntity.ok(result);
    }

    /**
     * 카테고리별 태그 목록을 조회합니다.
     */
    @GetMapping("/categories/{category}")
    @Operation(summary = "카테고리별 태그 조회", description = "특정 카테고리의 태그 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<List<TagResponse>> getTagsByCategory(@PathVariable TagCategory category) {
        List<DesignTag> tags = designTagRepository.findByCategoryAndApproved(category, true);
        List<TagResponse> response = tags.stream()
                .map(TagResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }
}
