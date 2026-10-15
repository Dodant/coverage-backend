-- Requirements: 20.1, 20.3 - 표지별 디자인 태그와 검색 색인 입력 데이터.
CREATE TABLE IF NOT EXISTS book_cover_tags (
    cover_id uuid NOT NULL REFERENCES book_covers(id) ON DELETE CASCADE,
    tag_id uuid NOT NULL REFERENCES design_tags(id) ON DELETE CASCADE,
    PRIMARY KEY (cover_id, tag_id)
);

CREATE INDEX IF NOT EXISTS idx_book_cover_tags_tag_id ON book_cover_tags(tag_id);
