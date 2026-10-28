# Book Cover Archive — Backend

REST API server for a book cover design archive: compare the covers of original books with their translated editions around the world, upload member redesigns, and share them through likes, comments and collections.

Java 21 · Spring Boot 3.2 · PostgreSQL · Redis · Elasticsearch · Kafka · S3-compatible storage

> How the system fits together: [ARCHITECTURE.md](ARCHITECTURE.md). Code conventions and Git rules: [CLAUDE.md](CLAUDE.md) (Korean). Functional requirements are in [`.kiro/specs/book-cover-archive/requirements.md`](.kiro/specs/book-cover-archive/requirements.md).

## Quick start

Requirements: JDK 21, Docker, Python 3 (for smoke tests only)

```bash
docker compose up -d     # Postgres 5432, Redis 6379, ES 9200, RustFS (S3) 9000/9001, Kafka 9092
./gradlew bootRun        # dev profile, http://localhost:8080/api
```

- Health check: http://localhost:8080/api/actuator/health
- Swagger UI: http://localhost:8080/api/swagger-ui.html
- Dev connection settings are in `application-dev.yml`, so no environment variables are needed. The schema is created automatically (`ddl-auto: update`).
- Elasticsearch installs the Korean analyzer plugin (nori) on first start, so it takes a while to become healthy.

## Creating accounts

```bash
# Sign up (role: PUBLISHER | AUTHOR | DESIGNER | MEMBER)
curl -X POST localhost:8080/api/auth/signup -H 'Content-Type: application/json' \
  -d '{"email":"me@test.com","password":"password123","displayName":"me","role":"MEMBER"}'

# Log in, then send the returned accessToken as: Authorization: Bearer <token>
curl -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"me@test.com","password":"password123"}'
```

ADMIN accounts cannot be created through sign-up. Locally, promote a user directly in the database. The JWT filter reloads the user from the database on every request, so existing tokens pick up the new role:

```bash
docker exec bookcover-postgres psql -U bookcover -d bookcover \
  -c "UPDATE users SET role='ADMIN' WHERE email='me@test.com';"
```

## API overview

Every path is prefixed with `/api`. See Swagger for the full spec.

| Path | Purpose | Auth |
|---|---|---|
| `/auth` | Sign up, log in, refresh token (single-use: each refresh returns a new one; clients must store it and must not refresh concurrently), log out (`POST /auth/logout` with the refresh token) | None |
| `/books/original`, `/books/translated`, `/books/{bookId}/covers` | Original books, translations, cover variants | GET is public |
| `/covers`, `/redesigns` | Cover image upload, redesigns | GET is public |
| `/search` (latest gallery: `GET /search/browse/latest?page=0&size=24`; other browsing under `/search/browse/...`), `/tags`, `/authors`, `/publishers`, `/designers`, `/series` | Search, browsing, entities | GET is public |
| `/likes`, `/comments`, `/collections` | Social features, collections | Required (except GET on public/shared collections) |
| `/users` | Profiles, account deletion (`DELETE /users/me` with `{"password"}`) | Required |
| `/reports` | Content reports | Required |
| `/takedowns` | Copyright takedown requests | POST open to non-members |
| `/admin/{reports,moderation,entities,dashboard,takedowns,search}` | Admin features | ADMIN |

Likes, comments, uploads, reports and takedown requests are rate limited. Exceeding a limit returns `429` with a `Retry-After` header. Limits are set under `rate-limit.*` in `application.yml`.

## Tests

```bash
./gradlew test                                                               # all tests (validation + rate-limit interceptor unit tests)
./gradlew test --tests 'com.bookcover.archive.validation.IsbnValidatorTest'  # a single class
```

To check real API behavior, run the smoke scripts against a running server. They exit non-zero on failure.

```bash
python3 scripts/smoke_report_takedown.py   # reports and takedown requests
python3 scripts/smoke_rate_limits.py       # rate limits (clears rate_limit:* keys in Redis first)
python3 scripts/smoke_cover_variants.py    # cover variants
```

## Production environment variables (prod profile)

Required when `SPRING_PROFILES_ACTIVE=prod`. Variables without a default must be set.

| Variable | Default |
|---|---|
| `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` | — |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | — / 6379 / empty |
| `ELASTICSEARCH_URIS` | — |
| `KAFKA_BOOTSTRAP_SERVERS` | — |
| `S3_ACCESS_KEY`, `S3_SECRET_KEY`, `S3_BUCKET_NAME` | — |
| `S3_ENDPOINT` / `S3_REGION` | empty (AWS) / ap-northeast-2 |
| `JWT_SECRET` | — |
| `JWT_EXPIRATION` / `JWT_REFRESH_EXPIRATION` (ms) | 86400000 / 604800000 |

Prod uses `ddl-auto: validate` and there is no migration tool, so the schema must be prepared separately.

## Troubleshooting

| Symptom | Cause / fix |
|---|---|
| A nonexistent path returns **500** instead of 404 | The catch-all `Exception` handler in `GlobalExceptionHandler` also catches routing failures. Check the path for typos first and verify it in Swagger. |
| `LazyInitializationException` on read APIs | Check whether `spring.jpa.open-in-view` was turned off. Controllers map entities to DTOs, so it must stay on. |
| Elasticsearch index setup fails on startup | The nori plugin isn't installed yet. Run `docker compose ps` and wait until ES is healthy. |
| Rate limits block local testing | `docker exec bookcover-redis sh -c "redis-cli --scan --pattern 'rate_limit:*' \| xargs -r redis-cli del"` |

### 백오피스 도서·표지 버전 관리

관리자 전용 `/api/admin/catalog`:
- `GET /`: `query`(제목/저자/ISBN), `kind`(ORIGINAL/TRANSLATED), `status`, `language`, `publisher`, `hasCover`, `issue`, `needsAttention`, `page`, `size`(최대 100), `sort`(created/updated/title). 상태를 생략하면 원서·번역서의 모든 상태를 조회한다. 기존 `/admin/books`의 초안 원서 계약은 유지된다.
- `GET /{id}`: 비공개 상태를 포함한 도서 상세·연결 도서·보완 항목. `GET /{id}/editions`는 원서 ID를 받아 원서와 번역서 전체 상세 목록을 반환한다.
- `PUT /{bookId}/covers/{coverId}`: 표지의 label, printingFrom, usedFrom, variantType, ribbonStatus, sourceName, sourceUrl 전체 교체. 선택 필드 null/빈 값은 비우기이며, credits는 null이면 유지·목록이면 역할별 크레딧 교체다. 소속 도서·관리자 권한을 검증하고 도서 변경 이력에 기록한다. 삭제된 도서는 수정하지 않는다.

`variantType`: UNKNOWN/INITIAL/RECOVER/SPECIAL/PRINTING/IMAGE_REPLACEMENT.
`ribbonStatus`: UNKNOWN/CLEAN/BANDED. 표지 변형 추가 API도 두 필드를 선택적으로 받는다.
`issue`: MISSING_COVER/HIDDEN_COVER/LOW_RESOLUTION/UNKNOWN_RESOLUTION/MISSING_ISBN/MISSING_SOURCE/MISSING_CREDIT/MISSING_YEAR/MISSING_PUBLISHER/UNCLASSIFIED_VARIANT.
해상도는 대표 표지의 800×1200 기준이며, 크기 미상·숨김을 저해상도로 오판하지 않는다. 보완은 검토 작업 표시이며 공개 여부를 바꾸지 않는다.

운영 배포 전에 `scripts/migrations/2026-10-27_cover_variant_metadata.sql`을 적용해야 한다. 기존 레코드의 새 필드는 UNKNOWN으로 응답한다. 디자인 묶음(`similarGroupId`)과 이미지 URL 동일성, 도서별 표지 소속은 서로 별도 관계다.
새 목록의 DB 필터·페이지 처리·표지 소유권·크레딧 교체는 `CatalogServiceImplTest`에서 H2로 검증한다(`./gradlew test --tests '*CatalogServiceImplTest'`). PostgreSQL 및 외부 서비스 연결을 요구하지 않는다.

### 관계 수정과 동일·유사 이미지 탐지

- `POST /api/admin/catalog/{id}/relationship-preview`: `{action, targetId}`로 영향을 받는 도서, 현재 originalId/workId/targetWorkId를 확인한다. action은 MOVE_TRANSLATION/JOIN_WORK/LEAVE_WORK. 분리 시 targetId는 null.
- `PUT /api/admin/catalog/{id}/relationship`: 같은 작업에 검토 결과의 `expectedOriginalId`, `expectedWorkId`, `expectedTargetWorkId`를 함께 보낸다. 도서를 정해진 순서로 잠그고 오래된 검토 상태·삭제 도서·공개 번역서의 비공개 원서 연결을 거절한다. 적용 시 감사 로그와 검색 갱신 이벤트를 기록한다.
- `GET /api/admin/image-inspection/matches?bookId=&kind=&page=`: bookId를 생략하면 전체 도서. kind는 EXACT_FILE/SAME_URL/SIMILAR. 페이지당 20쌍이며 파일 SHA-256 일치, URL 일치, dHash 거리 6 이하(종횡비 오차 3% 이하) 순서로 구분한다. 유사 해시는 검토 후보이며 동일성 확정·자동 병합에 쓰지 않는다.
- `POST /api/admin/image-inspection/scan`: `{after: null, limit: 10}`부터 시작하고 결과의 nextCursor를 다음 요청의 after로 보낸다. hasMore=false면 끝이다. 파일별 독립 트랜잭션으로 해시를 기록하고 실패에는 coverId/bookId/message를 반환한다. 관리자 권한 필요. 숨김·삭제·차단 표지 제외. URL은 S3 관리 경로만 허용하며 파일 크기 10MB·이미지 4천만 픽셀 이하만 분석한다.

새 업로드에는 자동으로 `file_sha256`, `perceptual_hash`를 기록한다. 구형 파일 분석으로 만든 기록은 원본 업로드 검증 기록으로 승격하지 않는다. 기존 `sha256`의 차단용 픽셀 해시 의미를 유지한다. 운영에서는 `scripts/migrations/2026-10-27_image_fingerprints.sql`을 먼저 적용한다. 비교는 등록된 표지의 모든 해시 쌍을 검사하므로 대규모 도입 시 후보 인덱스/백그라운드 잡으로 확장한다.

관계 변경 회귀 테스트: `./gradlew test --tests '*RelationshipServiceConcurrencyTest'`. 두 개의 별도 트랜잭션으로 교차 번역서 이동과 동일 번역서의 경쟁 이동을 검증한다. 이전 원서를 포함한 전체 수정 대상을 같은 순서로 잠그고, 잠금 후 1차 캐시를 갱신해 검토 ID를 다시 검사한다. 작품 연결 변경의 번역서에는 검색 이벤트만 발행한다.

업로드 검증과 해시 계산은 `ImageValidator.MAX_PIXELS`(4천만 픽셀)를 공유한다. 초과 이미지는 래스터 디코딩·해시 계산·S3 저장 전에 거절하며, 최소 해상도 면제 계정에도 최대 픽셀 제한은 적용된다. 허용된 파일은 기존처럼 본문 디코딩을 검증하고, 해시와 썸네일 인코딩이 완료된 뒤 S3에 기록한다. `ImageUploadLimitsTest`는 초과 PNG의 400 응답과 저장소 무호출을 검증한다.
