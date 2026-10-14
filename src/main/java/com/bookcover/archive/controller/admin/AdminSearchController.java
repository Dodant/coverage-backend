package com.bookcover.archive.controller.admin;

import com.bookcover.archive.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 검색 인덱스 관리 API.
 * 문서 구조가 바뀐 배포 뒤(예: designer → designers/designerIds)나 색인이 DB와 어긋났을 때 전체를 다시 색인한다.
 */
@RestController
@RequestMapping("/admin/search")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Search", description = "관리자 검색 인덱스 관리 API")
@SecurityRequirement(name = "bearerAuth")
public class AdminSearchController {

    private final SearchService searchService;

    public AdminSearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * 전체 재색인. 기존 문서를 지우고 원서·번역서·리디자인을 모두 다시 넣는다.
     */
    // ponytail: 요청 스레드에서 동기 실행하고 도중에는 검색 결과가 비어 보인다. 책이 수만 권을 넘으면 비동기 작업 + 별칭 인덱스 교체로
    @PostMapping("/reindex")
    @Operation(summary = "검색 인덱스 전체 재색인", description = "기존 문서를 지우고 모든 책·리디자인을 다시 색인합니다. 새 필드 매핑은 먼저 PUT _mapping으로 넣어야 합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "재색인 완료"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<Void> reindex() {
        searchService.reindexAll();
        return ResponseEntity.noContent().build();
    }
}
