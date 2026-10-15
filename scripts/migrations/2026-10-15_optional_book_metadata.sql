-- 요구사항 1.1, 2.2 변경: 나라·연도·장르를 모르면 비워 둘 수 있다. prod 배포 전 실행 (옛 앱은 항상 값을 넣으므로 영향 없음).
ALTER TABLE media ALTER COLUMN year DROP NOT NULL;
ALTER TABLE media ALTER COLUMN country_code DROP NOT NULL;
ALTER TABLE original_books ALTER COLUMN genre DROP NOT NULL;
