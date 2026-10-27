package com.bookcover.archive.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 이미지 처리 서비스 인터페이스.
 * S3/MinIO 업로드, 썸네일 생성, 색상 추출, sRGB 변환 기능을 제공합니다.
 *
 * Requirements:
 * - 14.7: 업로드된 이미지를 표준 색공간(sRGB)으로 변환하여 저장
 * - 14.8: 썸네일과 원본 이미지를 별도로 저장하여 로딩 성능 최적화
 * - 20.2: 대표 색상 추출 로직
 */
public interface ImageService {

    /**
     * 이미지를 업로드하고 썸네일을 생성합니다.
     *
     * @param file 업로드할 이미지 파일
     * @param type 이미지 유형 (COVER, REDESIGN, PROFILE)
     * @return 업로드 결과 (원본 URL, 썸네일 URL, 크기 정보)
     */
    ImageUploadResult uploadImage(MultipartFile file, ImageType type);

    /** 검증된 업로드의 URL 쌍과 서버가 기록한 메타데이터를 확인한다. */
    void requireVerifiedImage(String imageUrl, String thumbnailUrl, ImageType type,
                              Integer width, Integer height, Long fileSize);

    /** 삭제 승인된 이미지를 차단 목록에 넣고 관리 중인 원본·썸네일을 제거한다. */
    void blockImage(String imageUrl, String thumbnailUrl);

    /** 관리 중인 저장 파일의 비교 해시를 계산한다. 이미 계산된 경우 false. */
    boolean indexFingerprint(String imageUrl);

    /**
     * 이미지 파일을 검증합니다.
     *
     * @param file 검증할 이미지 파일
     * @return 검증 결과
     */
    ImageValidationResult validateImage(MultipartFile file);

    /**
     * 이미지에서 대표 색상을 추출합니다.
     *
     * @param imageUrl 이미지 URL
     * @return HEX 형식의 대표 색상 (예: #FF5733)
     */
    String extractDominantColor(String imageUrl);

    /**
     * 이미지를 삭제합니다.
     *
     * @param imageUrl 삭제할 이미지 URL
     */
    void deleteImage(String imageUrl);

    /**
     * 이미지 유형 열거형
     */
    enum ImageType {
        COVER,
        REDESIGN,
        PROFILE
    }

    /**
     * 이미지 업로드 결과 레코드
     */
    record ImageUploadResult(
            String originalUrl,
            String thumbnailUrl,
            int width,
            int height,
            long fileSize
    ) {}

    /**
     * 이미지 검증 결과 레코드
     */
    record ImageValidationResult(
            boolean valid,
            java.util.List<String> errors
    ) {}
}
