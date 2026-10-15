package com.bookcover.archive.repository;

import com.bookcover.archive.domain.book.ImageAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

/** Requirements: 14.1, 17.3 - 검증된 이미지 및 차단 해시 조회. */
public interface ImageAssetRepository extends JpaRepository<ImageAsset, UUID> {
    Optional<ImageAsset> findByImageUrl(String imageUrl);
    boolean existsBySha256AndBlockedTrue(String sha256);
    List<ImageAsset> findAllBySha256(String sha256);
}
