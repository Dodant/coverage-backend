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

### 도서 관계 수정

- `POST /api/admin/catalog/{id}/relationship-preview`: `{action, targetId}`로 영향을 받는 도서, 현재 originalId/workId/targetWorkId를 확인한다. action은 MOVE_TRANSLATION/JOIN_WORK/LEAVE_WORK. 분리 시 targetId는 null.
- `PUT /api/admin/catalog/{id}/relationship`: 같은 작업에 검토 결과의 `expectedOriginalId`, `expectedWorkId`, `expectedTargetWorkId`를 함께 보낸다. 도서를 정해진 순서로 잠그고 오래된 검토 상태·삭제 도서·공개 번역서의 비공개 원서 연결을 거절한다. 적용 시 감사 로그와 검색 갱신 이벤트를 기록한다.
