# API 계약

엔드포인트 목록과 응답·페이징·권한 규칙. 요청/응답 필드의 정식 스펙은 실행 중인 서버의 Swagger(`/api/swagger-ui.html`)가 기준이고, 이 문서는 **Swagger에서 보이지 않는 규칙**(접근 권한, 레이트 리밋, 상태 필터, 비즈니스 제약)을 적는다.

- 모든 경로 앞에 context-path `/api`가 붙는다. 아래 표는 `/api`를 뺀 경로다.
- 접근 규칙의 단일 출처는 `src/main/java/com/bookcover/archive/config/security/SecurityConfig.java`다. 여기 표는 그 규칙을 컨트롤러에 대입한 결과다.
- 표기: **공개**=비로그인 허용, **인증**=로그인 필요, **ADMIN**=관리자. RL=`@RateLimited` 액션.

## 공통 규칙

### 접근 권한 (SecurityConfig)

| 규칙 | 경로 |
|---|---|
| 전부 공개 | `/auth/**`, Swagger, `/actuator/health` |
| POST 공개 | `/takedowns` |
| GET 공개 | `/books/**`, `/covers/**`, `/redesigns/**`, `/search/**`, `/browse/**`(매칭되는 컨트롤러 없음), `/authors/**`, `/publishers/**`, `/designers/**`, `/series/**`, `/tags/**`, `/collections/public/**`, `/collections/shared/**` |
| ADMIN | `/admin/**` (+ `controller/admin/*` 클래스는 `@PreAuthorize("hasRole('ADMIN')")`도 붙음) |
| 인증 | 그 외 전부 (`anyRequest().authenticated()`) |

GET 공개 패턴 안에 있어도 핸들러가 `userDetails.getId()`를 부르면 비로그인 시 500이 난다(`GET /redesigns/my`). [KNOWN_ISSUES.md](KNOWN_ISSUES.md) 참고.

### 응답 형식

- 봉투(wrapper) 없음. 단건은 DTO, 목록은 Spring `Page<T>` 기본 JSON(`content`, `totalElements`, `totalPages`, `pageable` …).
- 검색·브라우징만 자체 레코드: `SearchResult {items, totalCount, facets, page, size, totalPages}`, `BrowseResult {category, categoryValue, totalCount, items, page, size, totalPages}`. `facets`는 현재 항상 비어 있다.
- `/admin/reports/target/.../count`, `/admin/takedowns/count`는 숫자 하나(`Long`)를 그대로 반환한다.
- 에러: `ErrorResponse {code, message, details, timestamp}` (`exception/GlobalExceptionHandler.java`). 상태 매핑:
  - `ValidationException`, `MethodArgumentNotValidException`, `IllegalArgumentException`, `IllegalStateException` → 400
  - `ResourceNotFoundException` → 404, `DuplicateIsbnException` → 409, `RateLimitExceededException` → 429 + `Retry-After`
  - 그 외 모든 예외(**매핑 안 된 경로 포함**) → 500 `INTERNAL_ERROR`
  - 서비스 레이어의 ADMIN 재검증 실패는 `ValidationException`이라 **403이 아니라 400**이다(Catalog/Relationship/ImageInspection/CoverGroup/Tag 서비스).

### 페이징

- 대부분 Spring `Pageable` + `@PageableDefault(size = 20)`: `?page=0&size=20&sort=field,desc`. 전역 최대 크기 설정이 없어 Spring 기본(2000)까지 허용된다.
- 기본 크기 예외: `/tags` 50, `/admin/books` 50(createdAt DESC), `/admin/dashboard/logs*` 50.
- 직접 `page`/`size`를 받는 곳: `/search/browse/latest`(기본 24, 1–60 밖이면 400), `/admin/catalog`(기본 30, 1–100으로 보정), `/admin/image-inspection/matches`(20 고정).
- 페이징 없음: `/series/{id}/books`, `/books/original/{id}/translations`, `/books/translated/by-original/{id}`, `/tags/categories/{category}`, `/tags/popular`(`limit` 상한 없음).

### 레이트 리밋

`@RateLimited`가 붙은 메서드만 센다. 한도는 `application.yml`의 `rate-limit.*`. 동작 방식은 [ARCHITECTURE.md#레이트-리밋](../ARCHITECTURE.md#레이트-리밋).

| 액션 | 한도 | 붙은 엔드포인트 |
|---|---|---|
| `UPLOAD` | 10/시간 | `POST /covers`, `POST /covers/redesign`, `POST /books/original`, `PUT /books/original/{id}`, `POST /books/translated/original/{originalBookId}`, `PUT /books/translated/{id}`, `POST /books/{bookId}/covers` |
| `COMMENT` | 30/분 | `POST /comments`, `PUT /comments/{id}` |
| `LIKE` | 100/분 | `POST /likes/{targetType}/{targetId}` |
| `REPORT` | 5/시간 | `POST /reports` |
| `TAKEDOWN` | 5/시간 | `POST /takedowns` (비로그인은 IP 단위) |

`POST /redesigns`, `POST /covers/validate`에는 리밋이 없다(리디자인은 이미지 업로드 단계에서 UPLOAD를 소모).

### 공개 조회의 상태 필터

- 책: `Media.isPublic()`(= `PUBLISHED`)인 것만 공개 GET에 노출(`BookServiceImpl`의 `getOriginalBook`/`getTranslatedBook`/번역서 목록, 저자·출판사별 목록 쿼리, 시리즈, 중복·유사 검색).
- 검색·브라우징: ES 쿼리에 `status=PUBLISHED` 필터. 인덱스에는 모든 상태 문서가 들어 있다.
- 표지 `GET /covers/{id}`: `cover.hidden`만 확인(소속 책 상태는 보지 않음).
- 리디자인: 단건은 `DELETED`만 제외, 목록은 상태 필터 없음 → [KNOWN_ISSUES.md](KNOWN_ISSUES.md).

## 엔드포인트

### 인증 `/auth` — 공개

| 메서드 | 경로 | 요청 | 비고 |
|---|---|---|---|
| POST | `/auth/signup` | `SignupRequest` | 201. role은 `PUBLISHER/AUTHOR/DESIGNER/MEMBER`만(ADMIN 거절) |
| POST | `/auth/login` | `LoginRequest` | 정지 기간이 지난 계정은 여기서 ACTIVE로 복구 |
| POST | `/auth/refresh` | `TokenRefreshRequest` | 리프레시 토큰 1회용. 새 토큰 쌍을 반환하므로 클라이언트는 동시에 갱신하면 안 된다 |
| POST | `/auth/logout` | `TokenRefreshRequest` | 204. 리프레시 토큰 폐기 |

### 책 — GET 공개, 쓰기 인증

누구나(로그인 사용자) 어떤 책이든 수정한다(위키 모델, 소유권 검사 없음). 수정은 감사 로그에 남는다. **삭제는 위키 모델 대상이 아니다** — 현재 검사가 빠져 있는 결함이다([KNOWN_ISSUES.md](KNOWN_ISSUES.md)).

| 메서드 | 경로 | RL | 비고 |
|---|---|---|---|
| POST | `/books/original` | UPLOAD | `CreateOriginalBookRequest`. `status=DRAFT`는 ADMIN만, 그 외 상태값은 400 |
| GET | `/books/original/{id}` | | PUBLISHED만 |
| PUT | `/books/original/{id}` | UPLOAD | `UpdateOriginalBookRequest` |
| DELETE | `/books/original/{id}` | | 소프트 삭제(DELETED). 소유권 검사 누락(결함) |
| GET | `/books/original/{id}/translations` | | 리스트 |
| GET | `/books/original/check-duplicate?isbn=` | | ISBN 중복 확인(공개 원서·번역서 모두) |
| GET | `/books/original/find-similar?title=&authorName=&year=` | | year 선택 |
| GET | `/books/original/by-author/{authorId}`, `/by-publisher/{publisherId}` | | Page |
| POST | `/books/translated/original/{originalBookId}` | UPLOAD | 비공개 원서에는 번역서를 달 수 없다. 번역서는 항상 PUBLISHED로 생성 |
| GET/PUT/DELETE | `/books/translated/{id}` | PUT=UPLOAD | |
| GET | `/books/translated/by-original/{originalBookId}` | | 리스트 |
| POST | `/books/{bookId}/covers` | UPLOAD | `AddCoverRequest` → 표지 변형 추가(같은 ISBN의 리커버·특정 쇄 표지) |
| PUT | `/books/{bookId}/covers/{coverId}/representative` | | 대표 표지 지정. 204 |

표지를 따로 조회하는 GET 목록은 없다. 책 응답의 `frontCover`/`backCover`/`covers`를 쓴다.

**이미지 URL 규칙**: 책·리디자인 생성/수정에 넣는 `imageUrl`/`thumbnailUrl`은 반드시 이 서버의 업로드 API(`POST /covers`, `/covers/redesign`) 응답 값이어야 한다. `ImageServiceImpl.requireVerifiedImage`가 `image_assets` 기록(URL·타입·썸네일·크기 일치, 차단 여부)을 검증하고 아니면 400.

**표지 메타데이터 필드**(`BookCoverResponse` 및 생성/수정 요청):
- `variantType`: `UNKNOWN/INITIAL/RECOVER/SPECIAL/PRINTING/IMAGE_REPLACEMENT`, `ribbonStatus`(띠지): `UNKNOWN/CLEAN/BANDED`. DB가 NULL이면 `UNKNOWN`으로 응답.
- `copyrightHolder`(최대 500자, 선택): 확인해 입력한 개인·단체명(공동 저작권자 가능). 제작 크레딧·출판사·이미지 출처와 별개. 수정 시 **null/생략 = 유지, 빈 문자열(공백 포함) = 비우기**, 앞뒤 공백은 제거. 구형 클라이언트가 필드를 몰라도 값이 지워지지 않게 하기 위한 규칙이며 관리자 표지 메타데이터 API도 같다.
- `credits`: 역할(`DESIGN/ART_DIRECTION/ILLUSTRATION/PHOTOGRAPHY/LETTERING`)별 디자이너. 표지당 최대 20개, 모르는 이름은 `Designer`를 새로 만든다.

### 이미지 업로드 `/covers`

| 메서드 | 경로 | RL | 권한 | 비고 |
|---|---|---|---|---|
| POST | `/covers` | UPLOAD | 인증 | multipart `file` → `ImageUploadResponse`(originalUrl, thumbnailUrl, …) |
| POST | `/covers/redesign` | UPLOAD | 인증 | 리디자인 이미지 |
| POST | `/covers/validate` | | 인증 | 저장 없이 검증만 |
| GET | `/covers/{id}` | | 공개 | 숨김 표지는 제외 |

검증 규칙(`validation/ImageValidator.java`): jpeg/png/webp, 10MB 이하, 800×1200 이상(면제 계정 제외), 4천만 픽셀 이하(면제 계정에도 적용). 차단(테이크다운)된 이미지와 픽셀이 같으면 거절.

### 리디자인 `/redesigns` — GET 공개, 쓰기 인증

| 메서드 | 경로 | 비고 |
|---|---|---|
| POST | `/redesigns` | `CreateRedesignRequest`. 원서·번역서 중 하나 이상 연결 필수 |
| GET/PUT/DELETE | `/redesigns/{id}` | 수정·삭제에 작성자 검사 누락(결함, 리디자인은 위키 모델 아님) |
| GET | `/redesigns/by-original/{id}`, `/by-translated/{id}`, `/by-user/{userId}` | Page |
| GET | `/redesigns/my` | 로그인 필요(비로그인 시 500) |

### 검색·브라우징 `/search`, 기여자, 태그 — 공개

| 메서드 | 경로 | 비고 |
|---|---|---|
| GET | `/search` | `query, countryCode, languageCode, yearFrom, yearTo, genre, publisherId, tagIds, sortBy(RELEVANCE 기본)` + Pageable |
| GET | `/search/browse/latest?page=0&size=24` | 최신 공개 갤러리(원서·번역서·리디자인, createdAt DESC) |
| GET | `/search/browse/{country/{code} \| year/{year} \| genre/{genre} \| tag/{tagId}}` | Pageable |
| GET | `/authors`, `/publishers`, `/designers`, `/series` (`?name=`) | Page |
| GET | `/authors/{id}`, `/publishers/{id}`, `/designers/{id}`, `/series/{id}` | 프로필 |
| GET | `/authors/{id}/books`, `/publishers/{id}/books` | Page, PUBLISHED만 |
| GET | `/designers/{id}/covers?role=` | 크레딧 기준 표지 1행씩(역할 포함) |
| GET | `/series/{id}/books` | 페이징 없음 |
| GET | `/tags`(`?name=&category=`), `/tags/{id}`, `/tags/{id}/covers`, `/tags/popular?limit=`, `/tags/categories/{category}` | |

### 소셜·컬렉션·사용자 — 인증 (예외 표기)

대상 타입: 좋아요·댓글·컬렉션 아이템 = `ORIGINAL_COVER | TRANSLATED_COVER | REDESIGN`, 신고 = `ORIGINAL_BOOK | TRANSLATED_BOOK | REDESIGN | COMMENT`.

| 메서드 | 경로 | RL | 비고 |
|---|---|---|---|
| POST | `/likes/{targetType}/{targetId}` | LIKE | 토글 |
| GET | `/likes/{targetType}/{targetId}/count`, `/status` | | 로그인 필요 |
| POST | `/comments` | COMMENT | |
| PUT/DELETE | `/comments/{commentId}` | PUT=COMMENT | 삭제는 하드 삭제 |
| GET | `/comments/{targetType}/{targetId}` | | 로그인 필요 |
| POST | `/collections` | | visibility 기본 PRIVATE (`PRIVATE/PUBLIC/LINK_ONLY`) |
| GET/PUT/DELETE | `/collections/{id}` | | 수정·삭제는 소유자만(서비스 검사) |
| POST/DELETE | `/collections/{id}/items[/{itemId}]` | | 소유자만 |
| GET | `/collections/my` | | |
| GET | `/collections/public`, `/collections/shared/{shareToken}` | | **공개** |
| POST | `/collections/{id}/duplicate` | | |
| POST | `/reports` | REPORT | 대상별 신고 3건이면 자동 HIDDEN |
| POST | `/takedowns` | TAKEDOWN | **비회원 허용**. 응답은 최소 영수증(`id, status, createdAt`) |
| GET/PUT | `/users/me` | | |
| PUT | `/users/me/password` | | |
| DELETE | `/users/me` | | 본문 `{"password"}`. 익명화 탈퇴 |
| GET | `/users/{userId}`, `/users/search?displayName=` | | 본인이면 전체, 아니면 공개 프로필 |
| GET | `/users` | | ADMIN(`@PreAuthorize`) |

## 관리자 API `/admin/**` — ADMIN

| 영역 | 엔드포인트 | 컨트롤러 |
|---|---|---|
| DRAFT 원서 | `GET /admin/books?status=DRAFT` | `AdminBookController` |
| 카탈로그 | `GET /admin/catalog`, `GET /admin/catalog/{id}`, `GET /admin/catalog/{id}/editions`, `PUT /admin/catalog/{bookId}/covers/{coverId}` | `AdminCatalogController` |
| 관계 수정 | `POST /admin/catalog/{id}/relationship-preview`, `PUT /admin/catalog/{id}/relationship` | `AdminRelationshipController` |
| 이미지 검사 | `POST /admin/image-inspection/scan`, `GET /admin/image-inspection/matches` | `AdminImageInspectionController` |
| 표지 묶기 | `PUT /admin/covers/{coverId}/similar-group` `{withCoverId}`(null이면 묶음 해제) | `AdminCoverController` |
| 태그 | `POST /admin/tags`, `PUT·DELETE /admin/covers/{coverId}/tags/{tagId}` | `controller/AdminTagController`(admin 패키지 밖) |
| 대시보드·감사 로그 | `GET /admin/dashboard/stats[/users\|/content\|/reports\|/takedowns]`, `GET /admin/dashboard/logs[/range\|/action/{a}\|/entity/{type}/{id}\|/user/{id}]` | `AdminDashboardController` |
| 기여자 병합·별칭 | `/admin/entities/{authors\|publishers\|designers}/{id}/aliases[/{alias}]`, `POST .../merge` | `AdminEntityController` |
| 모더레이션 | `POST /admin/moderation/content/{hide\|restore\|delete}`, `POST /admin/moderation/users/{id}/{suspend\|restrict-reporting\|unrestrict-reporting}` | `AdminModerationController` |
| 신고 | `GET /admin/reports/queue`, `GET /admin/reports/target/{type}/{id}[/count]`, `POST /admin/reports/{id}/review` | `AdminReportController` |
| 테이크다운 | `GET /admin/takedowns[/queue\|/{id}\|/count]`, `POST /admin/takedowns/{id}/{approve\|reject}` | `AdminTakedownController` |
| 검색 재색인 | `POST /admin/search/reindex` | `AdminSearchController` |

DRAFT 원서 공개는 별도 API 없이 `POST /admin/moderation/content/restore`로 한다(PUBLISHED로 바꿈).

### 백오피스 도서·표지 버전 관리 `/admin/catalog`

- `GET /`: `query`(제목/저자/ISBN), `kind`(ORIGINAL/TRANSLATED), `status`, `language`, `publisher`, `hasCover`, `issue`, `needsAttention`, `page`, `size`(최대 100), `sort`(created/updated/title). 상태를 생략하면 원서·번역서의 모든 상태를 조회한다. 기존 `/admin/books`의 초안 원서 계약은 유지된다.
- `GET /{id}`: 비공개 상태를 포함한 도서 상세·연결 도서·보완 항목. `GET /{id}/editions`는 원서 ID를 받아 원서와 번역서 전체 상세 목록을 반환한다.
- `PUT /{bookId}/covers/{coverId}`: 표지의 label, printingFrom, usedFrom, variantType, ribbonStatus, sourceName, sourceUrl 전체 교체. 선택 필드 null/빈 값은 비우기이며(`copyrightHolder`는 위 규칙대로 null=유지), credits는 null이면 유지·목록이면 역할별 크레딧 교체다. 소속 도서·관리자 권한을 검증하고 도서 변경 이력에 기록한다. 삭제된 도서는 수정하지 않는다.
- `issue`: `MISSING_COVER/HIDDEN_COVER/LOW_RESOLUTION/UNKNOWN_RESOLUTION/MISSING_ISBN/MISSING_SOURCE/MISSING_CREDIT/MISSING_YEAR/MISSING_PUBLISHER/UNCLASSIFIED_VARIANT`. 해상도는 대표 표지의 800×1200 기준이며, 크기 미상·숨김을 저해상도로 오판하지 않는다. 보완은 검토 작업 표시이며 공개 여부를 바꾸지 않는다.
- 디자인 묶음(`similarGroupId`)과 이미지 URL 동일성, 도서별 표지 소속은 서로 별도 관계다.

### 관계 수정 (원서↔번역서, 작품 묶음)

- `POST /admin/catalog/{id}/relationship-preview`: `{action, targetId}`로 영향을 받는 도서, 현재 originalId/workId/targetWorkId를 확인한다. action은 `MOVE_TRANSLATION`(번역서를 다른 원서로) / `JOIN_WORK`(같은 작품의 다른 판으로 묶기, 대상에 Work가 없으면 생성) / `LEAVE_WORK`(분리, targetId=null).
- `PUT /admin/catalog/{id}/relationship`: 같은 요청에 미리보기 결과의 `expectedOriginalId`, `expectedWorkId`, `expectedTargetWorkId`를 함께 보낸다. 잠근 뒤 값이 달라졌으면 거절(오래된 검토 결과). 삭제 도서, 공개 번역서를 비공개 원서에 연결하는 경우도 거절. 적용 시 감사 로그와 검색 갱신 이벤트를 기록한다. 잠금 방식은 [ARCHITECTURE.md](../ARCHITECTURE.md#관리자-백오피스-기능).

### 동일·유사 이미지 탐지

- `GET /admin/image-inspection/matches?bookId=&kind=&page=`: bookId 생략 시 전체. kind는 `EXACT_FILE`(파일 SHA-256 일치) / `SAME_URL` / `SIMILAR`(dHash 해밍 거리 6 이하 + 종횡비 오차 3% 이하). 페이지당 20쌍. 유사 해시는 검토 후보이며 동일성 확정·자동 병합에 쓰지 않는다.
- `POST /admin/image-inspection/scan`: `{after: null, limit: 10}`(limit 1–10)부터 시작하고 응답의 `nextCursor`를 다음 `after`로 보낸다. `hasMore=false`면 끝. 파일별 독립 트랜잭션으로 해시를 기록하고 실패는 `coverId/bookId/message`로 반환. 숨김·삭제·차단 표지 제외, S3 관리 경로 URL만, 10MB·4천만 픽셀 이하만 분석. 구형 파일 분석 결과는 원본 업로드 검증 기록으로 승격하지 않는다.

### 테이크다운 승인의 효과

`POST /admin/takedowns/{id}/approve`(`TakedownServiceImpl.approveRequest`)는 상태만 바꾸지 않고 콘텐츠를 실제로 내린다.
1. 대상 결정: 요청의 targetId → `targetUrl` 마지막 경로의 UUID → 표지 이미지/썸네일 URL → 리디자인 이미지 URL 순. 못 찾으면 400.
2. 책이면 모든 표지(변형·앞·뒤)를 숨김 + 이미지 차단, 책은 HIDDEN. 표지 하나면 그 표지를 숨기고 **소속 책 전체도 HIDDEN**. 리디자인이면 차단 + HIDDEN.
3. 이미지 차단(`ImageServiceImpl.blockImage`)은 같은 픽셀 해시의 모든 `image_assets`를 blocked로 표시하고 **S3 원본·썸네일을 삭제**한다. 같은 픽셀의 재업로드는 거절된다.

`restore`로 책을 다시 PUBLISHED로 바꿔도 표지 숨김·이미지 차단은 풀리지 않는다(파일도 이미 삭제됨).

### 검색 재색인

`POST /admin/search/reindex`: 인덱스 문서를 전부 지우고 DB의 원서·번역서·리디자인(모든 상태)을 요청 스레드에서 동기로 다시 넣는다. 도중에는 검색 결과가 비어 보인다. 새 ES 필드는 이 호출 전에 매핑부터 넣어야 한다([DEVELOPMENT.md](DEVELOPMENT.md#elasticsearch-필드-추가)).
