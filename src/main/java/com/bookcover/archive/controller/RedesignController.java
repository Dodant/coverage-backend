package com.bookcover.archive.controller;

import com.bookcover.archive.config.security.CustomUserDetails;
import com.bookcover.archive.controller.dto.book.RedesignResponse;
import com.bookcover.archive.domain.book.Redesign;
import com.bookcover.archive.exception.ResourceNotFoundException;
import com.bookcover.archive.exception.ValidationException;
import com.bookcover.archive.service.RedesignService;
import com.bookcover.archive.service.dto.CreateRedesignRequest;
import com.bookcover.archive.service.dto.UpdateRedesignRequest;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * 리디자인 API 컨트롤러.
 * 리디자인 CRUD 엔드포인트를 제공합니다.
 *
 * Requirements: 3.1, 3.4
 */
@RestController
@RequestMapping("/redesigns")
@Tag(name = "Redesigns", description = "리디자인 관련 API")
public class RedesignController {

    private final RedesignService redesignService;

    public RedesignController(RedesignService redesignService) {
        this.redesignService = redesignService;
    }

    /**
     * 리디자인을 등록합니다.
     *
     * Requirements: 3.1 - 리디자인을 원서 또는 번역서와 연결
     */
    @PostMapping
    @Operation(summary = "리디자인 등록", description = "새 리디자인을 등록합니다. 원서 또는 번역서 중 하나와 연결해야 합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패)"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "원서/번역서 없음")
    })
    public ResponseEntity<RedesignResponse> createRedesign(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateRedesignRequest request) {
        if (!request.hasValidBookReference()) {
            throw new ValidationException("원서 또는 번역서 중 하나는 반드시 지정해야 합니다",
                    Map.of("originalBookId", "null", "translatedBookId", "null"));
        }

        CreateRedesignRequest requestWithUser = new CreateRedesignRequest(
                request.imageUrl(),
                request.thumbnailUrl(),
                request.description(),
                request.originalBookId(),
                request.translatedBookId(),
                userDetails.getId()
        );
        Redesign redesign = redesignService.createRedesign(requestWithUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(RedesignResponse.from(redesign));
    }

    /**
     * 리디자인을 조회합니다.
     */
    @GetMapping("/{id}")
    @Operation(summary = "리디자인 조회", description = "리디자인 상세 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "리디자인 없음")
    })
    public ResponseEntity<RedesignResponse> getRedesign(@PathVariable UUID id) {
        Redesign redesign = redesignService.getRedesign(id)
                .orElseThrow(() -> new ResourceNotFoundException("Redesign", id));
        return ResponseEntity.ok(RedesignResponse.from(redesign));
    }

    /**
     * 리디자인을 수정합니다.
     */
    @PutMapping("/{id}")
    @Operation(summary = "리디자인 수정", description = "리디자인 정보를 수정합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리디자인 없음")
    })
    public ResponseEntity<RedesignResponse> updateRedesign(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRedesignRequest request) {
        Redesign redesign = redesignService.updateRedesign(id, request);
        return ResponseEntity.ok(RedesignResponse.from(redesign));
    }

    /**
     * 리디자인을 삭제합니다.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "리디자인 삭제", description = "리디자인을 삭제합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리디자인 없음")
    })
    public ResponseEntity<Void> deleteRedesign(@PathVariable UUID id) {
        redesignService.deleteRedesign(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 원서의 리디자인 목록을 조회합니다.
     *
     * Requirements: 3.4 - 각 원서/번역서에 연결된 모든 리디자인 목록을 조회
     */
    @GetMapping("/by-original/{originalBookId}")
    @Operation(summary = "원서별 리디자인 조회", description = "특정 원서에 연결된 모든 리디자인을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<Page<RedesignResponse>> getRedesignsByOriginalBook(
            @PathVariable UUID originalBookId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Redesign> redesigns = redesignService.getRedesignsByOriginalBook(originalBookId, pageable);
        Page<RedesignResponse> response = redesigns.map(RedesignResponse::from);
        return ResponseEntity.ok(response);
    }

    /**
     * 번역서의 리디자인 목록을 조회합니다.
     *
     * Requirements: 3.4 - 각 원서/번역서에 연결된 모든 리디자인 목록을 조회
     */
    @GetMapping("/by-translated/{translatedBookId}")
    @Operation(summary = "번역서별 리디자인 조회", description = "특정 번역서에 연결된 모든 리디자인을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<Page<RedesignResponse>> getRedesignsByTranslatedBook(
            @PathVariable UUID translatedBookId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Redesign> redesigns = redesignService.getRedesignsByTranslatedBook(translatedBookId, pageable);
        Page<RedesignResponse> response = redesigns.map(RedesignResponse::from);
        return ResponseEntity.ok(response);
    }

    /**
     * 사용자의 리디자인 목록을 조회합니다.
     */
    @GetMapping("/by-user/{userId}")
    @Operation(summary = "사용자별 리디자인 조회", description = "특정 사용자가 업로드한 모든 리디자인을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<Page<RedesignResponse>> getRedesignsByUser(
            @PathVariable UUID userId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Redesign> redesigns = redesignService.getRedesignsByUser(userId, pageable);
        Page<RedesignResponse> response = redesigns.map(RedesignResponse::from);
        return ResponseEntity.ok(response);
    }

    /**
     * 내 리디자인 목록을 조회합니다.
     */
    @GetMapping("/my")
    @Operation(summary = "내 리디자인 조회", description = "현재 로그인한 사용자의 모든 리디자인을 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<Page<RedesignResponse>> getMyRedesigns(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Redesign> redesigns = redesignService.getRedesignsByUser(userDetails.getId(), pageable);
        Page<RedesignResponse> response = redesigns.map(RedesignResponse::from);
        return ResponseEntity.ok(response);
    }
}
