package com.bookcover.archive.controller;

import com.bookcover.archive.config.security.CustomUserDetails;
import com.bookcover.archive.domain.collection.Collection;
import com.bookcover.archive.domain.collection.CollectionItem;
import com.bookcover.archive.domain.collection.CollectionItemType;
import com.bookcover.archive.domain.collection.CollectionVisibility;
import com.bookcover.archive.exception.ResourceNotFoundException;
import com.bookcover.archive.exception.ValidationException;
import com.bookcover.archive.service.CollectionService;
import com.bookcover.archive.service.dto.CreateCollectionRequest;
import com.bookcover.archive.service.dto.UpdateCollectionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 컬렉션 API 컨트롤러.
 * 컬렉션 CRUD, 아이템 관리, 공유 엔드포인트를 제공합니다.
 *
 * Requirements: 11.1, 11.9, 11.11
 */
@RestController
@RequestMapping("/collections")
@Tag(name = "Collections", description = "컬렉션 관련 API")
public class CollectionController {

    private final CollectionService collectionService;

    public CollectionController(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    /**
     * 새 컬렉션을 생성합니다.
     *
     * Requirements: 11.1 - 회원이 새 컬렉션을 생성하면 컬렉션 이름, 설명, 공개/비공개 설정을 저장한다
     */
    @PostMapping
    @Operation(summary = "컬렉션 생성", description = "새 컬렉션을 생성합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "컬렉션 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<CollectionResponse> createCollection(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateCollectionApiRequest request) {
        CreateCollectionRequest serviceRequest = new CreateCollectionRequest(
                userDetails.getId(),
                request.name(),
                request.description(),
                request.visibility() != null ? request.visibility() : CollectionVisibility.PRIVATE
        );
        Collection collection = collectionService.createCollection(serviceRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(CollectionResponse.from(collection));
    }

    /**
     * 컬렉션을 조회합니다.
     */
    @GetMapping("/{id}")
    @Operation(summary = "컬렉션 조회", description = "컬렉션 상세 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "비공개 컬렉션"),
            @ApiResponse(responseCode = "404", description = "컬렉션 없음")
    })
    public ResponseEntity<CollectionResponse> getCollection(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id) {
        Collection collection = collectionService.getCollection(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collection", id));

        // 비공개 컬렉션은 소유자만 조회 가능
        if (collection.getVisibility() == CollectionVisibility.PRIVATE) {
            if (userDetails == null || !collection.getOwner().getId().equals(userDetails.getId())) {
                throw new ValidationException("비공개 컬렉션입니다", Map.of("collectionId", id));
            }
        }

        return ResponseEntity.ok(CollectionResponse.from(collection));
    }

    /**
     * 공유 토큰으로 컬렉션을 조회합니다.
     *
     * Requirements: 11.9 - 공개 컬렉션에 대해 공유 링크를 제공한다
     */
    @GetMapping("/shared/{shareToken}")
    @Operation(summary = "공유 링크로 컬렉션 조회", description = "공유 토큰으로 컬렉션을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "컬렉션 없음")
    })
    public ResponseEntity<CollectionResponse> getCollectionByShareToken(@PathVariable String shareToken) {
        Collection collection = collectionService.getCollectionByShareToken(shareToken)
                .orElseThrow(() -> new ResourceNotFoundException("Collection", "shareToken", shareToken));

        // PRIVATE 컬렉션은 공유 토큰으로도 접근 불가
        if (collection.getVisibility() == CollectionVisibility.PRIVATE) {
            throw new ResourceNotFoundException("Collection", "shareToken", shareToken);
        }

        return ResponseEntity.ok(CollectionResponse.from(collection));
    }

    /**
     * 컬렉션을 수정합니다.
     */
    @PutMapping("/{id}")
    @Operation(summary = "컬렉션 수정", description = "컬렉션 정보를 수정합니다. 소유자만 수정할 수 있습니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "컬렉션 없음")
    })
    public ResponseEntity<CollectionResponse> updateCollection(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCollectionApiRequest request) {
        UpdateCollectionRequest serviceRequest = new UpdateCollectionRequest(
                request.name(),
                request.description(),
                request.visibility()
        );
        Collection collection = collectionService.updateCollection(id, userDetails.getId(), serviceRequest);
        return ResponseEntity.ok(CollectionResponse.from(collection));
    }

    /**
     * 컬렉션을 삭제합니다.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "컬렉션 삭제", description = "컬렉션을 삭제합니다. 소유자만 삭제할 수 있습니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "컬렉션 없음")
    })
    public ResponseEntity<Void> deleteCollection(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id) {
        collectionService.deleteCollection(id, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * 컬렉션에 아이템을 추가합니다.
     */
    @PostMapping("/{id}/items")
    public ResponseEntity<CollectionResponse> addItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody AddItemRequest request) {
        collectionService.addItem(id, userDetails.getId(), request.itemType(), request.itemId());
        Collection collection = collectionService.getCollection(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collection", id));
        return ResponseEntity.ok(CollectionResponse.from(collection));
    }

    /**
     * 컬렉션에서 아이템을 제거합니다.
     */
    @DeleteMapping("/{id}/items/{itemId}")
    @Operation(summary = "컬렉션에서 아이템 제거", description = "컬렉션에서 아이템을 제거합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "제거 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "컬렉션 또는 아이템 없음")
    })
    public ResponseEntity<CollectionResponse> removeItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id,
            @PathVariable UUID itemId) {
        collectionService.removeItem(id, userDetails.getId(), itemId);
        Collection collection = collectionService.getCollection(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collection", id));
        return ResponseEntity.ok(CollectionResponse.from(collection));
    }

    /**
     * 내 컬렉션 목록을 조회합니다.
     */
    @GetMapping("/my")
    @Operation(summary = "내 컬렉션 목록", description = "현재 로그인한 사용자의 컬렉션 목록을 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<Page<CollectionSummaryResponse>> getMyCollections(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Collection> collections = collectionService.getUserCollections(userDetails.getId(), pageable);
        Page<CollectionSummaryResponse> response = collections.map(CollectionSummaryResponse::from);
        return ResponseEntity.ok(response);
    }

    /**
     * 공개 컬렉션 목록을 조회합니다.
     */
    @GetMapping("/public")
    @Operation(summary = "공개 컬렉션 목록", description = "공개 컬렉션 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<Page<CollectionSummaryResponse>> getPublicCollections(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Collection> collections = collectionService.getPublicCollections(pageable);
        Page<CollectionSummaryResponse> response = collections.map(CollectionSummaryResponse::from);
        return ResponseEntity.ok(response);
    }

    /**
     * 컬렉션을 복제합니다.
     *
     * Requirements: 11.11 - 다른 회원의 공개 컬렉션을 내 컬렉션으로 복제하는 기능을 제공한다
     */
    @PostMapping("/{id}/duplicate")
    @Operation(summary = "컬렉션 복제", description = "공개 컬렉션을 내 컬렉션으로 복제합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "복제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "비공개 컬렉션"),
            @ApiResponse(responseCode = "404", description = "컬렉션 없음")
    })
    public ResponseEntity<CollectionResponse> duplicateCollection(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id) {
        Collection duplicated = collectionService.duplicateCollection(id, userDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(CollectionResponse.from(duplicated));
    }

    // Request/Response DTOs

    /**
     * 컬렉션 생성 API 요청 DTO.
     */
    public record CreateCollectionApiRequest(
            @NotBlank(message = "컬렉션 이름은 필수입니다")
            @Size(max = 100, message = "컬렉션 이름은 100자를 초과할 수 없습니다")
            String name,

            @Size(max = 500, message = "설명은 500자를 초과할 수 없습니다")
            String description,

            CollectionVisibility visibility
    ) {}

    /**
     * 컬렉션 수정 API 요청 DTO.
     */
    public record UpdateCollectionApiRequest(
            @Size(max = 100, message = "컬렉션 이름은 100자를 초과할 수 없습니다")
            String name,

            @Size(max = 500, message = "설명은 500자를 초과할 수 없습니다")
            String description,

            CollectionVisibility visibility
    ) {}

    /**
     * 아이템 추가 요청 DTO.
     */
    public record AddItemRequest(
            @NotNull(message = "아이템 유형은 필수입니다")
            CollectionItemType itemType,

            @NotNull(message = "아이템 ID는 필수입니다")
            UUID itemId
    ) {}

    /**
     * 컬렉션 응답 DTO (상세).
     */
    public record CollectionResponse(
            UUID id,
            String name,
            String description,
            CollectionVisibility visibility,
            String shareToken,
            UUID ownerId,
            String ownerName,
            List<CollectionItemResponse> items,
            int itemCount,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static CollectionResponse from(Collection collection) {
            List<CollectionItemResponse> items = collection.getItems().stream()
                    .map(CollectionItemResponse::from)
                    .toList();
            return new CollectionResponse(
                    collection.getId(),
                    collection.getName(),
                    collection.getDescription(),
                    collection.getVisibility(),
                    collection.getShareToken(),
                    collection.getOwner().getId(),
                    collection.getOwner().getDisplayName(),
                    items,
                    items.size(),
                    collection.getCreatedAt(),
                    collection.getUpdatedAt()
            );
        }
    }

    /**
     * 컬렉션 요약 응답 DTO (목록용).
     */
    public record CollectionSummaryResponse(
            UUID id,
            String name,
            String description,
            CollectionVisibility visibility,
            UUID ownerId,
            String ownerName,
            int itemCount,
            Instant createdAt
    ) {
        public static CollectionSummaryResponse from(Collection collection) {
            return new CollectionSummaryResponse(
                    collection.getId(),
                    collection.getName(),
                    collection.getDescription(),
                    collection.getVisibility(),
                    collection.getOwner().getId(),
                    collection.getOwner().getDisplayName(),
                    collection.getItems().size(),
                    collection.getCreatedAt()
            );
        }
    }

    /**
     * 컬렉션 아이템 응답 DTO.
     */
    public record CollectionItemResponse(
            UUID id,
            CollectionItemType itemType,
            UUID itemId,
            Instant addedAt
    ) {
        public static CollectionItemResponse from(CollectionItem item) {
            return new CollectionItemResponse(
                    item.getId(),
                    item.getItemType(),
                    item.getItemId(),
                    item.getAddedAt()
            );
        }
    }
}
