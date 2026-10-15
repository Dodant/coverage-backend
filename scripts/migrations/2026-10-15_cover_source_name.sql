-- 표지 이미지 제공처 이름과 긴 출처 URL. prod 배포 전 실행 (옛 앱은 두 컬럼을 몰라도 동작한다).
ALTER TABLE book_covers ADD COLUMN IF NOT EXISTS source_name varchar(100);
ALTER TABLE book_covers ALTER COLUMN source_url TYPE varchar(1000);
