package com.bookcover.archive.controller;

import com.bookcover.archive.controller.dto.book.BookCoverResponse;
import com.bookcover.archive.domain.book.BookCover;
import com.bookcover.archive.exception.ValidationException;
import com.bookcover.archive.repository.BookCoverRepository;
import com.bookcover.archive.service.ImageService;
import com.bookcover.archive.service.ImageService.ImageType;
import com.bookcover.archive.service.ImageService.ImageUploadResult;
import com.bookcover.archive.service.ImageService.ImageValidationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

/**
 * 커버 이미지 API 컨트롤러.
 * 커버 이미지 업로드 및 조회 엔드포인트를 제공합니다.
 *
 * Requirements: 14.1, 14.8
 */
@RestController
@RequestMapping("/covers")
@Tag(name = "Covers", description = "커버 이미지 관련 API")
public class CoverController {

    private final ImageService imageService;
    private final BookCoverRepository bookCoverRepository;

    public CoverController(ImageService imageService, BookCoverRepository bookCoverRepository) {
        this.imageService = imageService;
        this.bookCoverRepository = bookCoverRepository;
    }

    /**
     * 커버 이미지를 업로드합니다.
     *
     * Requirements: 14.1 - 최소 해상도(800x1200 픽셀 이상) 검증
     * Requirements: 14.8 - 썸네일과 원본 이미지를 별도로 저장
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "커버 이미지 업로드", description = "북커버 이미지를 업로드합니다. 최소 해상도 800x1200, 최대 10MB, JPEG/PNG/WebP 형식만 허용됩니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "업로드 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (이미지 검증 실패)"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<ImageUploadResponse> uploadCoverImage(
            @RequestParam("file") MultipartFile file) {
        ImageValidationResult validationResult = imageService.validateImage(file);
        if (!validationResult.valid()) {
            throw new ValidationException("이미지 검증 실패",
                    Map.of("errors", validationResult.errors()));
        }

        ImageUploadResult uploadResult = imageService.uploadImage(file, ImageType.COVER);

        ImageUploadResponse response = new ImageUploadResponse(
                uploadResult.originalUrl(),
                uploadResult.thumbnailUrl(),
                file.getOriginalFilename(),
                uploadResult.width(),
                uploadResult.height(),
                uploadResult.fileSize()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 리디자인 이미지를 업로드합니다.
     */
    @PostMapping(value = "/redesign", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "리디자인 이미지 업로드", description = "리디자인 이미지를 업로드합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "업로드 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (이미지 검증 실패)"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<ImageUploadResponse> uploadRedesignImage(
            @RequestParam("file") MultipartFile file) {
        ImageValidationResult validationResult = imageService.validateImage(file);
        if (!validationResult.valid()) {
            throw new ValidationException("이미지 검증 실패",
                    Map.of("errors", validationResult.errors()));
        }

        ImageUploadResult uploadResult = imageService.uploadImage(file, ImageType.REDESIGN);

        ImageUploadResponse response = new ImageUploadResponse(
                uploadResult.originalUrl(),
                uploadResult.thumbnailUrl(),
                file.getOriginalFilename(),
                uploadResult.width(),
                uploadResult.height(),
                uploadResult.fileSize()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 이미지 검증만 수행합니다.
     */
    @PostMapping(value = "/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "이미지 검증", description = "이미지 파일의 유효성을 검증합니다. 업로드 전 사전 검증에 사용합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검증 완료")
    })
    public ResponseEntity<ImageValidationResponse> validateImage(
            @RequestParam("file") MultipartFile file) {
        ImageValidationResult result = imageService.validateImage(file);
        return ResponseEntity.ok(new ImageValidationResponse(result.valid(), result.errors()));
    }

    /**
     * 커버 정보를 조회합니다.
     */
    @GetMapping("/{id}")
    @Operation(summary = "커버 조회", description = "커버 상세 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "커버 없음")
    })
    public ResponseEntity<BookCoverResponse> getCover(@PathVariable UUID id) {
        BookCover cover = bookCoverRepository.findById(id)
                .orElseThrow(() -> new com.bookcover.archive.exception.ResourceNotFoundException("BookCover", id));
        return ResponseEntity.ok(BookCoverResponse.from(cover));
    }

    /**
     * 이미지 업로드 응답 DTO.
     */
    public record ImageUploadResponse(
            String originalUrl,
            String thumbnailUrl,
            String originalFileName,
            int width,
            int height,
            long fileSize
    ) {}

    /**
     * 이미지 검증 응답 DTO.
     */
    public record ImageValidationResponse(
            boolean valid,
            java.util.List<String> errors
    ) {}
}
