-- 기존 표지는 미입력(NULL)으로 유지한다. 출판사·디자이너·출처에서 추정하지 않는다.
ALTER TABLE book_covers ADD COLUMN IF NOT EXISTS copyright_holder varchar(500);
