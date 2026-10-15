package com.bookcover.archive.controller.admin;

import com.bookcover.archive.controller.dto.book.BookCoverResponse;
import com.bookcover.archive.service.CoverGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 표지 관리 API. 언어가 달라도 비슷한 표지를 묶는다(백오피스 '표지 묶기').
 */
@RestController
@RequestMapping("/admin/covers")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Covers", description = "관리자 표지 관리 API")
@SecurityRequirement(name = "bearerAuth")
public class AdminCoverController {

    private final CoverGroupService coverGroupService;

    public AdminCoverController(CoverGroupService coverGroupService) {
        this.coverGroupService = coverGroupService;
    }

    /** withCoverId가 null이면 묶음에서 뺀다 */
    public record SimilarGroupRequest(UUID withCoverId) {}

    @PutMapping("/{coverId}/similar-group")
    @Operation(summary = "비슷한 표지 묶기", description = "표지를 withCoverId 표지의 묶음으로 옮깁니다. withCoverId가 null이면 묶음에서 뺍니다. 1개만 남은 묶음은 풀립니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경된 표지"),
            @ApiResponse(responseCode = "400", description = "같은 표지끼리 묶기"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "표지 없음")
    })
    public ResponseEntity<BookCoverResponse> moveToGroup(@PathVariable UUID coverId, @RequestBody SimilarGroupRequest request) {
        return ResponseEntity.ok(BookCoverResponse.from(coverGroupService.moveToGroup(coverId, request.withCoverId())));
    }
}
