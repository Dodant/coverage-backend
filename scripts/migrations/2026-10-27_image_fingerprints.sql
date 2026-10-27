-- 기존 차단용 sha256의 의미와 값은 유지한다.
ALTER TABLE image_assets ADD COLUMN IF NOT EXISTS file_sha256 varchar(64);
ALTER TABLE image_assets ADD COLUMN IF NOT EXISTS perceptual_hash varchar(16);
CREATE INDEX IF NOT EXISTS idx_image_assets_file_sha256 ON image_assets(file_sha256);
