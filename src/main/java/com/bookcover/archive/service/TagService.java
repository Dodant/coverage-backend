package com.bookcover.archive.service;

import com.bookcover.archive.domain.tag.DesignTag;
import com.bookcover.archive.domain.tag.TagCategory;
import java.util.UUID;

/** Requirements: 20.1, 20.4 - 디자인 태그 사전과 커버 연결. */
public interface TagService {
    DesignTag createTag(String name, TagCategory category);
    void assignTag(UUID coverId, UUID tagId);
    void removeTag(UUID coverId, UUID tagId);
}
