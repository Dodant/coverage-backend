package com.bookcover.archive.controller.admin;

import com.bookcover.archive.config.security.CustomUserDetails;
import com.bookcover.archive.controller.dto.admin.AliasRequest;
import com.bookcover.archive.controller.dto.admin.AliasResponse;
import com.bookcover.archive.controller.dto.admin.MergeEntityRequest;
import com.bookcover.archive.domain.contributor.Author;
import com.bookcover.archive.domain.contributor.Designer;
import com.bookcover.archive.domain.contributor.Publisher;
import com.bookcover.archive.service.EntityManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.UUID;

/**
 * 관리자 엔티티 관리 API 컨트롤러.
 * 엔티티 병합 및 별칭 관리 엔드포인트를 제공합니다.
 *
 * Requirements: 19.5 - 엔티티 병합 기능을 제공한다
 * Requirements: 19.6 - 출판사/작가/디자이너에 대한 별칭(alias) 관리 기능을 제공한다
 */
@RestController
@RequestMapping("/admin/entities")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Entities", description = "관리자 엔티티 관리 API")
@SecurityRequirement(name = "bearerAuth")
public class AdminEntityController {

    private final EntityManagementService entityManagementService;

    public AdminEntityController(EntityManagementService entityManagementService) {
        this.entityManagementService = entityManagementService;
    }

    // 작가 별칭 관리

    /**
     * 작가의 별칭 목록을 조회합니다.
     */
    @GetMapping("/authors/{authorId}/aliases")
    @Operation(summary = "작가 별칭 조회", description = "작가의 별칭 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "작가 없음")
    })
    public ResponseEntity<Set<String>> getAuthorAliases(@PathVariable UUID authorId) {
        Set<String> aliases = entityManagementService.getAuthorAliases(authorId);
        return ResponseEntity.ok(aliases);
    }

    /**
     * 작가에 별칭을 추가합니다.
     */
    @PostMapping("/authors/{authorId}/aliases")
    @Operation(summary = "작가 별칭 추가", description = "작가에 별칭을 추가합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "추가 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "작가 없음")
    })
    public ResponseEntity<AliasResponse> addAuthorAlias(
            @PathVariable UUID authorId,
            @Valid @RequestBody AliasRequest request) {
        Author author = entityManagementService.addAuthorAlias(authorId, request.alias());
        return ResponseEntity.ok(new AliasResponse(author.getId(), author.getName(), author.getAliases()));
    }

    /**
     * 작가에서 별칭을 제거합니다.
     */
    @DeleteMapping("/authors/{authorId}/aliases/{alias}")
    @Operation(summary = "작가 별칭 제거", description = "작가에서 별칭을 제거합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "제거 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "작가 없음")
    })
    public ResponseEntity<AliasResponse> removeAuthorAlias(
            @PathVariable UUID authorId,
            @PathVariable String alias) {
        Author author = entityManagementService.removeAuthorAlias(authorId, alias);
        return ResponseEntity.ok(new AliasResponse(author.getId(), author.getName(), author.getAliases()));
    }

    // 출판사 별칭 관리

    /**
     * 출판사의 별칭 목록을 조회합니다.
     */
    @GetMapping("/publishers/{publisherId}/aliases")
    @Operation(summary = "출판사 별칭 조회", description = "출판사의 별칭 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "출판사 없음")
    })
    public ResponseEntity<Set<String>> getPublisherAliases(@PathVariable UUID publisherId) {
        Set<String> aliases = entityManagementService.getPublisherAliases(publisherId);
        return ResponseEntity.ok(aliases);
    }

    /**
     * 출판사에 별칭을 추가합니다.
     */
    @PostMapping("/publishers/{publisherId}/aliases")
    @Operation(summary = "출판사 별칭 추가", description = "출판사에 별칭을 추가합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "추가 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "출판사 없음")
    })
    public ResponseEntity<AliasResponse> addPublisherAlias(
            @PathVariable UUID publisherId,
            @Valid @RequestBody AliasRequest request) {
        Publisher publisher = entityManagementService.addPublisherAlias(publisherId, request.alias());
        return ResponseEntity.ok(new AliasResponse(publisher.getId(), publisher.getName(), publisher.getAliases()));
    }

    /**
     * 출판사에서 별칭을 제거합니다.
     */
    @DeleteMapping("/publishers/{publisherId}/aliases/{alias}")
    @Operation(summary = "출판사 별칭 제거", description = "출판사에서 별칭을 제거합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "제거 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "출판사 없음")
    })
    public ResponseEntity<AliasResponse> removePublisherAlias(
            @PathVariable UUID publisherId,
            @PathVariable String alias) {
        Publisher publisher = entityManagementService.removePublisherAlias(publisherId, alias);
        return ResponseEntity.ok(new AliasResponse(publisher.getId(), publisher.getName(), publisher.getAliases()));
    }

    // 디자이너 별칭 관리

    /**
     * 디자이너의 별칭 목록을 조회합니다.
     */
    @GetMapping("/designers/{designerId}/aliases")
    @Operation(summary = "디자이너 별칭 조회", description = "디자이너의 별칭 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "디자이너 없음")
    })
    public ResponseEntity<Set<String>> getDesignerAliases(@PathVariable UUID designerId) {
        Set<String> aliases = entityManagementService.getDesignerAliases(designerId);
        return ResponseEntity.ok(aliases);
    }

    /**
     * 디자이너에 별칭을 추가합니다.
     */
    @PostMapping("/designers/{designerId}/aliases")
    @Operation(summary = "디자이너 별칭 추가", description = "디자이너에 별칭을 추가합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "추가 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "디자이너 없음")
    })
    public ResponseEntity<AliasResponse> addDesignerAlias(
            @PathVariable UUID designerId,
            @Valid @RequestBody AliasRequest request) {
        Designer designer = entityManagementService.addDesignerAlias(designerId, request.alias());
        return ResponseEntity.ok(new AliasResponse(designer.getId(), designer.getName(), designer.getAliases()));
    }

    /**
     * 디자이너에서 별칭을 제거합니다.
     */
    @DeleteMapping("/designers/{designerId}/aliases/{alias}")
    @Operation(summary = "디자이너 별칭 제거", description = "디자이너에서 별칭을 제거합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "제거 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "디자이너 없음")
    })
    public ResponseEntity<AliasResponse> removeDesignerAlias(
            @PathVariable UUID designerId,
            @PathVariable String alias) {
        Designer designer = entityManagementService.removeDesignerAlias(designerId, alias);
        return ResponseEntity.ok(new AliasResponse(designer.getId(), designer.getName(), designer.getAliases()));
    }

    // 엔티티 병합

    /**
     * 두 작가를 병합합니다.
     */
    @PostMapping("/authors/merge")
    @Operation(summary = "작가 병합", description = "두 작가를 병합합니다. source 작가의 모든 책을 target 작가로 이전하고 source를 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "병합 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "작가 없음")
    })
    public ResponseEntity<Void> mergeAuthors(
            @Valid @RequestBody MergeEntityRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        entityManagementService.mergeAuthors(request.sourceId(), request.targetId(), userDetails.getId());
        return ResponseEntity.ok().build();
    }

    /**
     * 두 출판사를 병합합니다.
     */
    @PostMapping("/publishers/merge")
    @Operation(summary = "출판사 병합", description = "두 출판사를 병합합니다. source 출판사의 모든 책을 target 출판사로 이전하고 source를 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "병합 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "출판사 없음")
    })
    public ResponseEntity<Void> mergePublishers(
            @Valid @RequestBody MergeEntityRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        entityManagementService.mergePublishers(request.sourceId(), request.targetId(), userDetails.getId());
        return ResponseEntity.ok().build();
    }

    /**
     * 두 디자이너를 병합합니다.
     */
    @PostMapping("/designers/merge")
    @Operation(summary = "디자이너 병합", description = "두 디자이너를 병합합니다. source 디자이너의 모든 커버를 target 디자이너로 이전하고 source를 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "병합 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "디자이너 없음")
    })
    public ResponseEntity<Void> mergeDesigners(
            @Valid @RequestBody MergeEntityRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        entityManagementService.mergeDesigners(request.sourceId(), request.targetId(), userDetails.getId());
        return ResponseEntity.ok().build();
    }
}
