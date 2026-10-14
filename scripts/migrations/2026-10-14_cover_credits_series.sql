-- 표지 크레딧·시리즈 도입 (feat/cover-credits-series) — 1단계: 배포 전 실행
-- dev(ddl-auto=update)는 앱이 테이블·컬럼을 만들지만 앞면 표지 book_id 백필과 designer_id 데이터 이전은 하지 않는다.
-- prod(ddl-auto=validate)는 배포 전에 이 파일을 실행한다. 옛 앱이 계속 쓰는 designer_id는 지우지 않는다.
-- 새 코드 배포가 끝나면 2단계 2026-10-14_drop_book_designer.sql을 실행한다 (그 사이 옛 앱이 넣은 디자이너도 다시 옮긴다).
--   docker exec -i bookcover-postgres psql -U <user> -d <db> < scripts/migrations/2026-10-14_cover_credits_series.sql
--
-- ES: 기존 book_covers 인덱스에는 새 필드 매핑이 자동으로 들어가지 않는다. 배포 전에(문서가 색인되기 전에) 실행:
--   curl -XPUT "$ELASTICSEARCH_URIS/book_covers/_mapping" -H 'Content-Type: application/json' -d '{"properties":{
--     "designers":{"type":"text","analyzer":"korean_analyzer","search_analyzer":"korean_analyzer"},
--     "designerIds":{"type":"keyword"}}}'
--   새 코드 배포 후 기존 문서를 채우려면 ADMIN 토큰으로: curl -XPOST $API/admin/search/reindex -H "Authorization: Bearer $TOKEN"

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
CREATE INDEX IF NOT EXISTS idx_media_series ON media (series_id);

-- 표지별 나라·언어 (c910b86)
ALTER TABLE book_covers ADD COLUMN IF NOT EXISTS country_code varchar(2);
ALTER TABLE book_covers ADD COLUMN IF NOT EXISTS language_code varchar(2);

CREATE TABLE IF NOT EXISTS cover_credits (
    id          uuid PRIMARY KEY,
    cover_id    uuid NOT NULL REFERENCES book_covers (id),
    designer_id uuid NOT NULL REFERENCES designers (id),
    role        varchar(20) NOT NULL
        CHECK (role IN ('DESIGN', 'ART_DIRECTION', 'ILLUSTRATION', 'PHOTOGRAPHY', 'LETTERING')),
    CONSTRAINT uk_cover_credits_cover_designer_role UNIQUE (cover_id, designer_id, role)
);
-- 유니크 인덱스는 cover_id가 앞이라 디자이너별 조회(작업 목록·병합)에 못 쓴다
CREATE INDEX IF NOT EXISTS idx_cover_credits_designer ON cover_credits (designer_id);

-- 다중 표지 지원(abb9bc5) 이전에 만든 책은 앞면 표지의 book_id가 비어 있다.
-- 작업 목록·검색 색인은 book_id로 표지의 책을 찾으므로 먼저 채운다.
UPDATE book_covers c SET book_id = b.id
FROM original_books b WHERE b.front_cover_id = c.id AND c.book_id IS NULL;
UPDATE book_covers c SET book_id = b.id
FROM translated_books b WHERE b.front_cover_id = c.id AND c.book_id IS NULL;

-- 책 단위 디자이너를 대표 앞면 표지의 DESIGN 크레딧으로 옮긴다.
-- 옛 앱이 쓰는 designer_id 컬럼은 2단계에서 지우지만, FK는 지금 지운다: 남아 있으면 새 앱의 디자이너 병합(source 삭제)이 FK 위반으로 실패한다.
-- 옮긴 (책, 디자이너)는 기록해 두고 2단계는 그 뒤에 바뀐 것만 옮긴다 (배포 후 지운 크레딧이 되살아나지 않게).
CREATE TABLE IF NOT EXISTS migrated_book_designers (book_id uuid NOT NULL, designer_id uuid NOT NULL, PRIMARY KEY (book_id, designer_id));

DO $$
DECLARE
    t text;
    fk record;
BEGIN
    FOREACH t IN ARRAY ARRAY['original_books', 'translated_books'] LOOP
        IF EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = t AND column_name = 'designer_id') THEN
            EXECUTE format(
                'INSERT INTO cover_credits (id, cover_id, designer_id, role) '
                'SELECT gen_random_uuid(), front_cover_id, designer_id, ''DESIGN'' FROM %I '
                'WHERE designer_id IS NOT NULL ON CONFLICT DO NOTHING', t);
            EXECUTE format(
                'INSERT INTO migrated_book_designers (book_id, designer_id) '
                'SELECT id, designer_id FROM %I WHERE designer_id IS NOT NULL ON CONFLICT DO NOTHING', t);
            FOR fk IN SELECT tc.constraint_name
                      FROM information_schema.table_constraints tc
                      JOIN information_schema.key_column_usage k
                        ON k.constraint_name = tc.constraint_name AND k.table_name = tc.table_name
                      WHERE tc.table_name = t AND tc.constraint_type = 'FOREIGN KEY' AND k.column_name = 'designer_id' LOOP
                EXECUTE format('ALTER TABLE %I DROP CONSTRAINT %I', t, fk.constraint_name);
            END LOOP;
        END IF;
    END LOOP;
END $$;

COMMIT;
