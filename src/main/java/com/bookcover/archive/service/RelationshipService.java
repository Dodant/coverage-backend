package com.bookcover.archive.service;

import com.bookcover.archive.service.dto.RelationshipRequest;
import java.util.List;
import java.util.UUID;

/** 원서·번역서 연결과 작품 내 판 관계를 검토한 뒤 변경한다. */
public interface RelationshipService {
    record Preview(CatalogService.Item source, CatalogService.Item target, UUID originalId, UUID workId,
                   UUID targetWorkId, List<CatalogService.Item> affectedBooks, String summary) {}
    Preview preview(UUID sourceId, RelationshipRequest request);
    void apply(UUID sourceId, RelationshipRequest request, UUID userId);
}
