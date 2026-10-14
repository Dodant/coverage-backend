-- 표지 크레딧·시리즈 도입 (feat/cover-credits-series) — 2단계: 새 코드 배포 후 실행
-- 1단계(2026-10-14_cover_credits_series.sql) 이후 옛 앱이 넣은 책 단위 디자이너를 한 번 더 크레딧으로 옮기고 컬럼을 지운다.
-- 이 스크립트를 실행하면 옛 코드로 롤백할 수 없다.
--   docker exec -i bookcover-postgres psql -U <user> -d <db> < scripts/migrations/2026-10-14_drop_book_designer.sql

BEGIN;

UPDATE book_covers c SET book_id = b.id
FROM original_books b WHERE b.front_cover_id = c.id AND c.book_id IS NULL;
UPDATE book_covers c SET book_id = b.id
FROM translated_books b WHERE b.front_cover_id = c.id AND c.book_id IS NULL;

-- 1단계 이후 옛 앱이 새로 넣거나 바꾼 디자이너만 옮긴다. 이미 지워진 디자이너(병합 등)는 건너뛴다
DO $$
DECLARE
    t text;
BEGIN
    FOREACH t IN ARRAY ARRAY['original_books', 'translated_books'] LOOP
        IF EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = t AND column_name = 'designer_id') THEN
            EXECUTE format(
                'INSERT INTO cover_credits (id, cover_id, designer_id, role) '
                'SELECT gen_random_uuid(), b.front_cover_id, b.designer_id, ''DESIGN'' FROM %I b '
                'JOIN designers d ON d.id = b.designer_id '
                'WHERE NOT EXISTS (SELECT 1 FROM migrated_book_designers m '
                '                  WHERE m.book_id = b.id AND m.designer_id = b.designer_id) '
                'ON CONFLICT DO NOTHING', t);
            EXECUTE format('ALTER TABLE %I DROP COLUMN designer_id', t);
        END IF;
    END LOOP;
END $$;

DROP TABLE IF EXISTS migrated_book_designers;

COMMIT;
