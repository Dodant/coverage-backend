package com.bookcover.archive.controller;

import com.bookcover.archive.controller.dto.book.OriginalBookResponse;
import com.bookcover.archive.controller.dto.contributor.DesignerProfileResponse;
import com.bookcover.archive.controller.dto.contributor.DesignerResponse;
import com.bookcover.archive.domain.book.OriginalBook;
import com.bookcover.archive.domain.contributor.Designer;
import com.bookcover.archive.exception.ResourceNotFoundException;
import com.bookcover.archive.repository.DesignerRepository;
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
 * 디자이너 API 컨트롤러.
 * 디자이너 프로필 및 작품 목록 조회 엔드포인트를 제공합니다.
 *
 * Requirements: 10.3, 10.4
 */
@RestController
@RequestMapping("/designers")
@Tag(name = "Designers", description = "디자이너 관련 API")
public class DesignerController {

    private final DesignerRepository designerRepository;
    private final OriginalBookRepository originalBookRepository;

    public DesignerController(DesignerRepository designerRepository, OriginalBookRepository originalBookRepository) {
        this.designerRepository = designerRepository;
        this.originalBookRepository = originalBookRepository;
    }

    /**
     * 디자이너 프로필을 조회합니다.
     *
     * Requirements: 10.4 - 디자이너 전용 프로필 페이지 제공
     * Requirements: 10.5 - 엔티티의 기본 정보와 관련 북커버 수 표시
     */
    @GetMapping("/{id}")
    @Operation(summary = "디자이너 프로필 조회", description = "디자이너의 프로필 정보와 관련 북커버 수를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "디자이너 없음")
    })
    public ResponseEntity<DesignerProfileResponse> getDesignerProfile(@PathVariable UUID id) {
        Designer designer = designerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Designer", id));
        long bookCount = originalBookRepository.countByDesignerId(id);
        return ResponseEntity.ok(DesignerProfileResponse.from(designer, bookCount));
    }

    /**
     * 디자이너의 작품 목록을 조회합니다.
     *
     * Requirements: 10.3 - 디자이너가 디자인한 모든 북커버 목록 표시
     */
    @GetMapping("/{id}/books")
    @Operation(summary = "디자이너 작품 목록 조회", description = "디자이너가 디자인한 모든 북커버 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "디자이너 없음")
    })
    public ResponseEntity<Page<OriginalBookResponse>> getDesignerBooks(
            @PathVariable UUID id,
            @PageableDefault(size = 20) Pageable pageable) {
        designerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Designer", id));
        Page<OriginalBook> books = originalBookRepository.findByDesignerId(id, pageable);
        Page<OriginalBookResponse> response = books.map(OriginalBookResponse::from);
        return ResponseEntity.ok(response);
    }

    /**
     * 디자이너 목록을 조회합니다.
     */
    @GetMapping
    @Operation(summary = "디자이너 목록 조회", description = "디자이너 목록을 페이징하여 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<Page<DesignerResponse>> getDesigners(
            @RequestParam(required = false) String name,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Designer> designers;
        if (name != null && !name.isBlank()) {
            designers = designerRepository.findByNameContainingIgnoreCase(name, pageable);
        } else {
            designers = designerRepository.findAll(pageable);
        }
        Page<DesignerResponse> response = designers.map(DesignerResponse::from);
        return ResponseEntity.ok(response);
    }
}
