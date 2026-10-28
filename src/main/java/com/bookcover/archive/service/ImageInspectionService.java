package com.bookcover.archive.service;

import java.util.List;
import java.util.UUID;

/** 파일 동일성은 확정 결과, 지각 해시 유사도는 검토 후보로 구분한다. */
public interface ImageInspectionService {
    record ScanFailure(UUID coverId, UUID bookId, String message) {}
    record ScanResult(int examined, int indexed, UUID nextCursor, boolean hasMore, List<ScanFailure> failures) {}
    record CoverRef(UUID coverId, UUID bookId, String title, String languageCode, String edition, String imageUrl, String thumbnailUrl) {}
    record Match(CoverRef left, CoverRef right, String kind, int distance) {}
    record Results(List<Match> matches, int totalMatches, int totalCovers, int indexedCovers, int page, int totalPages) {}
    ScanResult scan(UUID after, int limit);
    Results matches(UUID bookId, String kind, int page);
}
