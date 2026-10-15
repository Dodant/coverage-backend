-- 비슷한 표지 묶음 (백오피스 '표지 묶기'). 같은 값을 가진 표지끼리 언어가 달라도 디자인이 비슷하다. prod 배포 전 실행
ALTER TABLE book_covers ADD COLUMN IF NOT EXISTS similar_group_id uuid;
CREATE INDEX IF NOT EXISTS idx_book_covers_similar_group_id ON book_covers (similar_group_id) WHERE similar_group_id IS NOT NULL;
