package com.bookcover.archive.controller;

import com.bookcover.archive.controller.dto.contributor.DesignerCoverResponse;
import com.bookcover.archive.controller.dto.contributor.DesignerProfileResponse;
import com.bookcover.archive.controller.dto.contributor.DesignerResponse;
import com.bookcover.archive.domain.book.BookCover;
import com.bookcover.archive.domain.book.CreditRole;
import com.bookcover.archive.domain.contributor.Designer;
import com.bookcover.archive.exception.ResourceNotFoundException;
import com.bookcover.archive.domain.media.Media;
import com.bookcover.archive.repository.BookCoverRepository;
import com.bookcover.archive.repository.DesignerRepository;
import com.bookcover.archive.repository.OriginalBookRepository;
import com.bookcover.archive.repository.TranslatedBookRepository;
import io.swagger.v3.oas.annotations.Operation;
import org.hibernate.Hibernate;
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
    private final BookCoverRepository bookCoverRepository;
    private final OriginalBookRepository originalBookRepository;
    private final TranslatedBookRepository translatedBookRepository;

    public DesignerController(DesignerRepository designerRepository, BookCoverRepository bookCoverRepository,
                              OriginalBookRepository originalBookRepository, TranslatedBookRepository translatedBookRepository) {
        this.designerRepository = designerRepository;
        this.bookCoverRepository = bookCoverRepository;
        this.originalBookRepository = originalBookRepository;
        this.translatedBookRepository = translatedBookRepository;
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
        long coverCount = bookCoverRepository.findCreditedCovers(id, null, Pageable.ofSize(1)).getTotalElements();
        return ResponseEntity.ok(DesignerProfileResponse.from(designer, coverCount));
    }

    /**
     * 디자이너가 크레딧에 오른 표지 목록을 조회합니다 (원서·번역서, 앞면 변형·뒷면 모두).
     *
     * Requirements: 10.3 - 디자이너가 디자인한 모든 북커버 목록 표시
     */
    @GetMapping("/{id}/covers")
    @Operation(summary = "디자이너 작업 표지 목록", description = "디자이너가 크레딧에 오른 표지를 최근 등록순으로 조회합니다. role로 역할을 거를 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "디자이너 없음")
    })
    public ResponseEntity<Page<DesignerCoverResponse>> getDesignerCovers(
            @PathVariable UUID id,
            @RequestParam(required = false) CreditRole role,
            @PageableDefault(size = 20) Pageable pageable) {
        designerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Designer", id));
        // ponytail: 표지마다 크레딧·책을 지연 로딩한다 (페이지당 수십 쿼리). 느려지면 fetch join이나 batch size로
        Page<DesignerCoverResponse> response = bookCoverRepository.findCreditedCovers(id, role, pageable)
                .map(cover -> DesignerCoverResponse.from(cover, id, ownerOf(cover)));
        return ResponseEntity.ok(response);
    }

    /** 앞면 표지는 book으로, 뒷면 표지는 book_id가 없어 원서·번역서의 backCover로 역조회한다 */
    private Media ownerOf(BookCover cover) {
        if (cover.getBook() != null) {
            // 지연 로딩 프록시는 Media 타입이라 instanceof OriginalBook이 거짓이 된다
            return (Media) Hibernate.unproxy(cover.getBook());
        }
        return originalBookRepository.findByBackCoverId(cover.getId()).<Media>map(b -> b)
                .or(() -> translatedBookRepository.findByBackCoverId(cover.getId()))
                .orElse(null);
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
