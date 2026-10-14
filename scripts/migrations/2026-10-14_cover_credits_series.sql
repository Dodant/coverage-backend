-- 표지 크레딧·시리즈 도입 (feat/cover-credits-series)
-- dev(ddl-auto=update)는 앱이 테이블·컬럼을 만들지만 기존 designer_id 데이터 이전과 컬럼 삭제는 하지 않는다.
-- prod(ddl-auto=validate)는 배포 전에 이 파일 전체를 실행해야 한다.
--   docker exec -i bookcover-postgres psql -U <user> -d <db> < scripts/migrations/2026-10-14_cover_credits_series.sql
--
-- ES: 기존 book_covers 인덱스에는 새 필드 매핑이 자동으로 들어가지 않는다. 배포 전에(문서가 색인되기 전에) 실행:
--   curl -XPUT "$ELASTICSEARCH_URIS/book_covers/_mapping" -H 'Content-Type: application/json' -d '{"properties":{
--     "designers":{"type":"text","analyzer":"korean_analyzer","search_analyzer":"korean_analyzer"},
--     "designerIds":{"type":"keyword"}}}'
--   기존 문서의 designers는 책을 수정하거나 표지를 추가할 때 다시 색인된다.

BEGIN;

CREATE TABLE IF NOT EXISTS series (
    id           uuid PRIMARY KEY,
    name         varchar(255) NOT NULL,
    publisher_id uuid REFERENCES publishers (id),
    created_at   timestamp(6) with time zone NOT NULL,
    updated_at   timestamp(6) with time zone NOT NULL
);

ALTER TABLE media ADD COLUMN IF NOT EXISTS series_id uuid REFERENCES series (id);
ALTER TABLE media ADD COLUMN IF NOT EXISTS series_number varchar(20);

CREATE TABLE IF NOT EXISTS cover_credits (
    id          uuid PRIMARY KEY,
    cover_id    uuid NOT NULL REFERENCES book_covers (id),
    designer_id uuid NOT NULL REFERENCES designers (id),
    role        varchar(20) NOT NULL
        CHECK (role IN ('DESIGN', 'ART_DIRECTION', 'ILLUSTRATION', 'PHOTOGRAPHY', 'LETTERING')),
    CONSTRAINT uk_cover_credits_cover_designer_role UNIQUE (cover_id, designer_id, role)
);

-- 책 단위 디자이너를 대표 앞면 표지의 DESIGN 크레딧으로 옮긴다
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'original_books' AND column_name = 'designer_id') THEN
        INSERT INTO cover_credits (id, cover_id, designer_id, role)
        SELECT gen_random_uuid(), front_cover_id, designer_id, 'DESIGN'
        FROM original_books WHERE designer_id IS NOT NULL
        ON CONFLICT DO NOTHING;
        ALTER TABLE original_books DROP COLUMN designer_id;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'translated_books' AND column_name = 'designer_id') THEN
        INSERT INTO cover_credits (id, cover_id, designer_id, role)
        SELECT gen_random_uuid(), front_cover_id, designer_id, 'DESIGN'
        FROM translated_books WHERE designer_id IS NOT NULL
        ON CONFLICT DO NOTHING;
        ALTER TABLE translated_books DROP COLUMN designer_id;
    END IF;
END $$;

COMMIT;
