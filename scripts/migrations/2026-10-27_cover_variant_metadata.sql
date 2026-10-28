-- 기존 표지는 미확인으로 취급한다. 기존 앱과 함께 사용 가능.
ALTER TABLE book_covers ADD COLUMN IF NOT EXISTS variant_type varchar(30);
ALTER TABLE book_covers ADD COLUMN IF NOT EXISTS ribbon_status varchar(20);
