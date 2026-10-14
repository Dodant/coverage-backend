# Architecture

How the backend is put together and how a request moves through it. For setup see [README.md](README.md); for conventions see [CLAUDE.md](CLAUDE.md).

## System overview

```
                         ┌──────────────────────── Spring Boot app (/api) ────────────────────────┐
 HTTP ──► JwtAuthenticationFilter ──► SecurityConfig rules ──► RateLimitInterceptor ──► Controller
                                                                     │                    │
                                                                  Redis               Service (tx)
                                                              rate_limit:*            │   │    │
                                                                                      │   │    └──► S3 / RustFS (images)
                                                                                      │   └──► Kafka producer
                                                                                Repository        │
                                                                                      │           ▼
                                                                                 PostgreSQL   Kafka listeners ──► Elasticsearch
                         └──────────────────────────────────────────────────────────────────────────┘
```

Single Spring Boot module, package `com.bookcover.archive`. PostgreSQL is the source of truth. Elasticsearch is a read model for search and browsing, fed asynchronously through Kafka.

| Component | Role | Where |
|---|---|---|
| PostgreSQL | All entities | `domain/`, `repository/` |
| Redis | Rate-limit counters only | `service/impl/RateLimitServiceImpl` |
| Elasticsearch (`book_covers` index, nori) | Full-text search, browse by country/year/genre/tag | `search/`, `service/impl/SearchServiceImpl` |
| Kafka | Content and moderation events | `event/`, `config/KafkaConfig` |
| S3 / RustFS | Cover and redesign images plus thumbnails | `service/impl/ImageServiceImpl`, `config/S3Config` |

## Request lifecycle

1. **`JwtAuthenticationFilter`** reads `Authorization: Bearer <jwt>`, takes the email from the token, **reloads the user from the DB** and sets a `CustomUserDetails` principal. Role and status changes therefore apply on the next request without issuing a new token. An invalid token is logged and the request continues as anonymous.
2. **`SecurityConfig`** decides access: public GETs, `/auth/**`, anonymous `POST /takedowns`, `/admin/**` requires ADMIN, everything else requires authentication. Sessions are stateless and CSRF is off.
3. **`RateLimitInterceptor`** acts only on handler methods annotated `@RateLimited(action = …)` (see [Rate limiting](#rate-limiting)).
4. **Controller** validates the request DTO with `@Valid` and calls a service. Controllers map entities to response DTOs **after** the service transaction has closed, which is why `spring.jpa.open-in-view` must stay `true`.
5. **Service** (`XxxService` interface + `impl/XxxServiceImpl`, class-level `@Transactional`) holds the business rules and re-checks ADMIN for moderation and takedown review.
6. **Errors** are thrown as domain exceptions and translated by `GlobalExceptionHandler` into `ErrorResponse { code, message, details, timestamp }`. The catch-all `Exception` handler also catches routing failures, so unknown paths return 500 instead of 404.

## Domain model

```
            Work (groups editions of one work)
              │ 1..*
              ▼
 Media (abstract, JOINED inheritance, table `media`)
   ├── OriginalBook ── author / publisher / designer, unique ISBN, genre, edition
   │      │ 1..*
   │      ▼
   └── TranslatedBook ── originalBook (required), author, translator, publisher, designer
         │
         ├─ frontCover (1:1, required) ─┐
         ├─ backCover  (1:1, optional) ─┼─► BookCover (image URL, thumbnail, dominant color, metadata)
         └─ covers     (1..*, variants) ┘     book_id → Media; label, printingFrom, usedFrom
                                                (recovers, printing-specific and special editions of one ISBN)

 Redesign ── createdBy User (required), points to an OriginalBook or a TranslatedBook
```

- **Content status** (`ContentStatus`): `DRAFT`, `PUBLISHED`, `HIDDEN`, `DELETED`. Applies to books, redesigns and comments; search only returns `PUBLISHED`.
- **Contributors**: `Author`, `Publisher`, `Designer` and `Translator` are separate entities with alias support. Admins merge duplicates through `EntityManagementService`.
- **Social and collections are polymorphic** by `(targetType, targetId)` rather than foreign keys:
  - `Like`, `Comment`, `CollectionItem` → `ORIGINAL_COVER | TRANSLATED_COVER | REDESIGN`
  - `Report` → `ORIGINAL_BOOK | TRANSLATED_BOOK | REDESIGN | COMMENT`

  Services check that the target exists (`validateLikeTargetExists` and similar) because the database cannot.
- **Denormalized counters** such as like, comment and contribution counts are updated with atomic `@Modifying @Query` UPDATEs, not read-modify-write.
- **Users**: `UserRole` = `PUBLISHER | AUTHOR | DESIGNER | MEMBER | ADMIN` (ADMIN cannot sign up). `UserStatus` = `ACTIVE | SUSPENDED | DELETED`. A suspension past `suspendedUntil` is lifted at login. `UserTitle` becomes `CONTRIBUTOR` once a MEMBER has registered 10 books (`ContributionServiceImpl`, a single conditional UPDATE).

## Asynchronous flows (Kafka)

| Topic | Produced by | Consumed by | Effect |
|---|---|---|---|
| `bookcover.content.events` | `BookServiceImpl`, `RedesignServiceImpl` on create/update/delete | `SearchIndexEventListener` (group `search-index-sync`) | Re-reads the entity from the DB and upserts or removes the ES document (`original_{id}`, `translated_{id}`, `redesign_{id}`) |
| `bookcover.moderation.events` | `ModerationServiceImpl` (reported, auto-hidden, …) | `ModerationEventListener` (group `moderation-notification`) | **Logs only.** Admin and owner notifications are TODO. |
| `bookcover.search.index` | `KafkaEventPublisher.publishSearchIndexEvent` exists, but no service calls it | nobody | Unused |

Payloads are JSON strings. Listeners catch and log every exception, so a failed event is dropped rather than retried.

## Rate limiting

- Counters are fixed windows in Redis, key `rate_limit:{action}:{subject}`. `INCR` and `EXPIRE` run in a single Lua script so concurrent requests cannot overshoot the limit and a key cannot lose its TTL.
- The subject is `user:{uuid}` when logged in, otherwise `ip:{remoteAddr}`. Prod sets `server.forward-headers-strategy: native` so the real client IP is used behind a proxy.
- If the request ends with a 4xx status, `afterCompletion` refunds one count, up to 3× the limit per window; failures beyond that count against the quota. 5xx responses and unhandled exceptions are not refunded.
- Actions and limits (`application.yml` → `rate-limit.*`): `UPLOAD` 10/h (covers and redesigns combined), `COMMENT` 30/min (create and edit), `LIKE` 100/min, `REPORT` 5/h, `TAKEDOWN` 5/h.
- An exceeded limit raises `RateLimitExceededException` → `429` with a `Retry-After` header.

## Moderation and takedowns

- **Reports**: a member reports content (`POST /reports`). When a target reaches **3 reports** (`AUTO_HIDE_THRESHOLD`), it is set to `HIDDEN` automatically and a `CONTENT_AUTO_HIDDEN` event is published. Admins work the queue at `/admin/reports/queue` and hide, restore, delete or suspend via `/admin/moderation`.
- **Takedowns**: rights holders, who may not be members, submit `POST /takedowns` anonymously and receive a minimal receipt (`id`, `status`, `createdAt`). Admins approve or reject at `/admin/takedowns`.
- **Audit log** (`AuditLog`, before/after values as JSON): at the moment only contributor merges in `EntityManagementServiceImpl` write to it.

## Image pipeline

`CoverController` → `ImageService.uploadImage`:

1. Validate with `ImageValidator`: format (jpeg, png, webp), minimum size 800×1200, maximum file size 10 MB (`image.*` in `application.yml`).
2. Convert to the sRGB color space.
3. Upload the original to `{type}/{uuid}.{ext}` and a 300×450 thumbnail to `{type}/thumbnails/{uuid}_thumb.{ext}`.
4. `extractDominantColor` derives the cover's representative color.

Spring's multipart limits are 10 MB per file and 15 MB per request.

## Known gaps (from code reading)

These are not yet reproduced at runtime, so verify each one before relying on it.

- **Hidden content may stay searchable.** Moderation changes status in PostgreSQL but does not publish a content event, so the ES document keeps `status=PUBLISHED` until the entity is next updated.
- **Indexing can race the commit.** Content events are sent from inside the service transaction, so the listener may read the DB before the commit and index stale data.
- Moderation notifications and most audit logging are not implemented (see above).
- `SecurityConfig` permits `GET /browse/**`, but browsing actually lives at `/search/browse/...`, so that rule matches nothing.
