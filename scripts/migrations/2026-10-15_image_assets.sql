-- 검증된 업로드와 저작권 차단 이미지 기록. prod 배포 전 실행.
CREATE TABLE IF NOT EXISTS image_assets (
    id uuid PRIMARY KEY,
    image_url varchar(1000) NOT NULL UNIQUE,
    thumbnail_url varchar(1000),
    sha256 varchar(64),
    type varchar(255),
    width integer,
    height integer,
    file_size bigint,
    blocked boolean NOT NULL DEFAULT false,
    created_at timestamp(6) with time zone NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_image_assets_blocked_sha256 ON image_assets (sha256) WHERE blocked = true;
ALTER TABLE book_covers ADD COLUMN IF NOT EXISTS hidden boolean NOT NULL DEFAULT false;
