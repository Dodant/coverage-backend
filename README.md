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
