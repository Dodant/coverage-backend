# CLAUDE.md

북커버 디자인 아카이브(원서·번역서 커버 비교, 리디자인, 소셜) REST API 백엔드.
Java 21 · Spring Boot 3.2.5 · Gradle · PostgreSQL · Redis · Elasticsearch(nori) · Kafka · S3 호환 스토리지(RustFS) · JWT(jjwt 0.12)

요구사항 원문: `.kiro/specs/book-cover-archive/{requirements,design,tasks}.md` — 코드·커밋의 "요구사항 N.M"은 여기 번호다.

## 작업 전에 읽을 문서

| 작업 | 먼저 볼 문서 |
|---|---|
| 엔드포인트 추가·권한·레이트 리밋·응답 형식 | [docs/API.md](docs/API.md), [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md#엔드포인트-추가변경) |
| 엔티티·컬럼·인덱스 변경, 마이그레이션 SQL | [docs/DATA_MODEL.md](docs/DATA_MODEL.md), [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md#엔티티-필드테이블-변경-스키마-변경) |
| 검색·ES 필드·Kafka 이벤트·outbox | [ARCHITECTURE.md](ARCHITECTURE.md#비동기-흐름-kafka), [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md#elasticsearch-필드-추가) |
| 이미지 업로드·해시·테이크다운 | [ARCHITECTURE.md](ARCHITECTURE.md#이미지-파이프라인), [docs/API.md](docs/API.md#테이크다운-승인의-효과) |
| 백오피스(관리자) API | [docs/API.md](docs/API.md#관리자-api-admin--admin) |
| 테스트 작성·실행, 배포 순서 | [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md#테스트) |
| 버그 수정 전 "이미 알려진 동작인가" 확인 | [docs/KNOWN_ISSUES.md](docs/KNOWN_ISSUES.md) |

영역별 규칙은 `.claude/rules/`(해당 경로 파일을 열면 자동 적용). 문서와 코드가 다르면 코드가 맞다 — 문서를 고친다.

## 명령어

```bash
docker compose up -d                 # Postgres/Redis/ES/RustFS/Kafka (ES는 첫 기동 시 nori 플러그인 설치)
./gradlew bootRun                    # 기본 프로필 dev, http://localhost:8080/api (context-path /api)
./gradlew build -x test              # 컴파일+패키징
./gradlew test                       # 전체 테스트 (docker 불필요: Mockito·H2)
./gradlew test --tests 'com.bookcover.archive.validation.IsbnValidatorTest'   # 단일 테스트
python3 scripts/smoke_rate_limits.py # 실행 중인 서버 대상 시나리오 검증 (smoke_*.py, 실패 시 non-zero, `BASE` 환경변수는 smoke_auth·smoke_credits_series만 지원)
```

- 헬스체크: `curl localhost:8080/api/actuator/health`, Swagger: `/api/swagger-ui.html`
- 책 자동 등록: `/register-book "<제목>"` 스킬(`.claude/skills/register-book/`, 에이전트 계정 + 면제 환경변수 필요). 실행 기록은 `api.py serve`(읽기 전용 JSON)를 띄우고 백오피스(`../coverage-backoffice`)에서 본다
- 린트/포맷: TODO — Checkstyle/Spotless 등 설정 없음
- 사람용 실행 가이드(계정 생성, ADMIN 승격, API 개요, prod 환경변수)는 `README.md`
- 구조·이벤트 흐름·설계 결정은 `ARCHITECTURE.md`, 그 외는 위 표의 `docs/*`

## 설정·환경변수

- `application.yml`(공통) + `application-{dev,test,prod}.yml`. `spring.profiles.active: dev`가 기본값.
- dev는 접속 정보가 yml에 하드코딩(docker-compose 값과 일치). `.env`/`.env.example` 없음. `application-local.yml`은 gitignore 대상.
- prod는 전부 환경변수: `DATABASE_URL/USERNAME/PASSWORD`, `REDIS_HOST/PORT/PASSWORD`, `ELASTICSEARCH_URIS`, `KAFKA_BOOTSTRAP_SERVERS`, `S3_*`, `JWT_SECRET`(필수), `JWT_EXPIRATION`, `JWT_REFRESH_EXPIRATION`.
- 레이트 리밋 한도는 `application.yml`의 `rate-limit.{upload,comment,like,report,takedown}`.
- 서지 자동 등록 에이전트 전용 계정은 사용자 ID로 면제한다: `RATE_LIMIT_EXEMPT_USER_IDS`(레이트 리밋), `IMAGE_RESOLUTION_EXEMPT_USER_IDS`(최소 해상도 800×1200). 쉼표 구분 UUID. 이메일은 탈퇴 후 재사용될 수 있어 쓰지 않는다.
- `ddl-auto`: dev=update, test=create-drop, prod=validate. 마이그레이션 도구 없음 — 스키마 변경 시 `scripts/migrations/<날짜>_<이름>.sql`을 멱등하게 작성하고 prod 배포 전에 수동 실행. 옛 앱이 쓰는 컬럼 삭제는 배포 후 실행하는 별도 스크립트로 나눈다(예: `2026-10-14_drop_book_designer.sql`). dev의 update는 컬럼 삭제·데이터 이전을 하지 않는다.
- ES 인덱스는 없을 때만 매핑과 함께 생성된다. 기존 인덱스에 필드를 추가하면 동적 매핑(standard 분석기)이 되므로 배포 전에 `PUT book_covers/_mapping`으로 먼저 넣는다. 기존 문서를 새 구조로 채우려면 배포 후 `POST /admin/search/reindex`(ADMIN, 동기·도중 검색 결과 비어 보임).
- `open-in-view: true`는 의도적이다. 컨트롤러가 서비스 트랜잭션 밖에서 엔티티→DTO 변환을 하므로 끄면 LazyInitializationException(500)이 난다.

## 아키텍처

패키지 `com.bookcover.archive` 단일 모듈, 레이어별 패키지:

```
controller/ (+ admin/, dto/)  →  service/ (인터페이스) + service/impl/ (*ServiceImpl)  →  repository/ (Spring Data JPA)
domain/<영역>/   엔티티·enum (book, user, social, report, takedown ...)
search/          ES 문서·리포지토리      event/  Kafka 이벤트·리스너
validation/      커스텀 제약 (@ValidUrl 등)  exception/  ErrorCode, GlobalExceptionHandler
config/security/ JWT·Security            config/ratelimit/  @RateLimited + 인터셉터
```

### 인증과 로그인 사용자 식별
- `JwtAuthenticationFilter`가 `Authorization: Bearer` 토큰에서 이메일을 꺼내 `CustomUserDetails`를 principal로 설정. Stateless, CSRF off.
- 토큰에 `typ`(access/refresh, 서로 대신 못 씀)과 `uid`(사용자 ID) 클레임이 있다. subject(이메일)만 믿으면 탈퇴 후 같은 이메일로 재가입한 계정에 옛 토큰이 통하므로 필터·갱신은 `JwtTokenProvider.isIssuedTo`로 ID 일치를 확인한다. 필터는 매 요청 DB에서 사용자를 다시 읽고 탈퇴했거나 정지 중이면 비로그인으로 처리한다. 정지 기간(`suspendedUntil`)이 지났으면 DB 상태가 아직 SUSPENDED여도 인증된다(`CustomUserDetails.isAccountNonLocked`, 로그인 시 ACTIVE로 복구).
- 리프레시 토큰은 1회용: 갱신·로그아웃 시 jti를 Redis `revoked_refresh:{jti}`(남은 유효기간 TTL)에 넣는다. 액세스 토큰은 폐기 수단이 없어 만료(24h)까지 유효하다.
- 탈퇴는 익명화(이메일을 `deleted-{id}@deleted.invalid`로 교체, 프로필 삭제, status=DELETED). 책·수정 이력·댓글은 남는다.
- **principal은 `CustomUserDetails`(String/UUID 아님).** 사용자 ID는 다음 두 가지로만 얻는다:
  - 컨트롤러 파라미터: `@AuthenticationPrincipal CustomUserDetails userDetails` → `userDetails.getId()`
  - 그 외(인터셉터 등): `SecurityUtils.getCurrentUserId()` (`Optional<UUID>`)
- 과거 인터셉터가 principal을 String으로 가정해 모든 요청을 비로그인으로 처리한 버그가 있었다(eb1e9be). 직접 파싱하지 말 것.
- 접근 규칙은 `SecurityConfig` 한 곳: 공개 GET 목록, `/admin/**`는 ADMIN, `POST /takedowns`는 비회원 허용, 나머지는 인증 필요. 새 공개 엔드포인트는 여기에 추가해야 한다.
- 서비스 레이어 ADMIN 재검증은 일부만: `TakedownServiceImpl`·`ModerationServiceImpl.reviewReport`는 DB 역할, Catalog/Relationship/ImageInspection/CoverGroup/Tag 서비스는 `SecurityUtils.hasRole("ADMIN")`(실패 시 `ValidationException` = 400). 새 관리자 기능은 `controller/admin/` + 클래스 `@PreAuthorize("hasRole('ADMIN')")`.

### 레이트 리밋 (공통 로직)
- `config/ratelimit/RateLimitInterceptor` — `WebMvcConfig`에서 전체 경로에 등록(Swagger/actuator 제외).
- 컨트롤러 메서드에 `@RateLimited(action = RateLimitAction.X)`를 붙여야만 적용된다. 설정만 있고 어노테이션이 없어 동작하지 않던 적이 있다(dbba5bf).
- 로그인 사용자는 `user:{id}`, 비로그인은 `ip:{remoteAddr}` 단위로 센다. 익명 엔드포인트도 어노테이션만 붙이면 된다(예: `POST /takedowns`).
- Lua 스크립트로 INCR+EXPIRE를 원자 실행. 4xx로 끝난 요청은 `afterCompletion`에서 1회 환불하되 윈도우당 한도×3회까지만(그 뒤 실패는 한도에 포함). 5xx·예외는 환불하지 않는다.
- Redis 키 `rate_limit:{action}:{subject}`. 초과 시 `RateLimitExceededException` → 429 + `Retry-After` 헤더.

### 에러 처리
- 서비스는 `ValidationException`, `ResourceNotFoundException`, `DuplicateIsbnException`, `RateLimitExceededException` 등을 던지고 `GlobalExceptionHandler`가 `ErrorResponse(ErrorCode, message, details)`로 변환.
- 새 에러 종류는 `ErrorCode` enum에 추가. 요청 DTO는 `@Valid` + jakarta 제약(메시지는 한국어).

## 코드 컨벤션

- 의존성 주입은 **생성자 직접 작성**(Lombok `@RequiredArgsConstructor` 사용 안 함).
- 서비스는 `XxxService` 인터페이스 + `impl/XxxServiceImpl`, 클래스 레벨 `@Transactional`.
- 일괄 갱신·카운터는 `@Modifying @Query` 단일 UPDATE로(N+1·동시성 이슈 수정 이력 있음).
- 요청 DTO는 컨트롤러 내부 `record`로 두는 경우가 많다(예: `TakedownController.CreateTakedownRequest`).
- 클래스 Javadoc에 관련 요구사항 번호를 단다:
  ```java
  /** ...설명.
   *
   * Requirements: 17.1, 17.2 - 요약
   */
  ```
- 주석·Javadoc은 한국어. 의도적으로 단순화한 부분은 `// ponytail:` 주석에 한계와 개선 경로를 적는다.

## 테스트

- 대부분 Mockito로 서비스를 직접 생성하는 단위 테스트(생성자 주입이라 컨텍스트 불필요). JPA·네이티브 SQL·잠금은 `@DataJpaTest` + H2 PostgreSQL 모드(`CatalogServiceImplTest`, `RelationshipServiceConcurrencyTest` 설정을 복사). `@SpringBootTest`는 없다.
- Testcontainers 의존성과 `application-test.yml`(포트 5433/6380/9201/9093/9002)은 있지만 쓰는 테스트도, 해당 포트를 띄우는 compose도 없다. TODO: 통합 테스트 실행 방법 미정.
- 상세(종류별 예, H2 설정, 스모크 목록)는 [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md#테스트).
- API 동작 검증은 `scripts/smoke_*.py`(서버 실행 중 + docker compose 필요, `docker exec bookcover-redis`로 레이트 리밋 키 초기화).

## Git 규칙

- 커밋 메시지는 **한국어**, 제목은 "무엇을 했는지" 한 줄, 관련 시 끝에 `(요구사항 18.1)`처럼 번호. 본문에 원인/변경 내용을 `-` 목록으로.
  - 예: `좋아요·댓글·업로드에 사용자 단위 레이트 리밋 적용 (요구사항 18.1)`
  - 예: `TranslatedBookRepository 누락된 import 추가 (main 컴파일 실패 수정)`
- 브랜치: `feat/<kebab-case>` (예: `feat/cover-variants`, `feat/report-takedown-endpoints`). main 기준 PR → merge commit.
- 초기 커밋(`N. 작업명 - [x] N.M ...`)은 `.kiro` tasks.md 체크리스트를 그대로 붙인 형식이다. 새 커밋에는 따라 하지 않는다.

## 주의사항 (실제로 겪은 것)

- **커밋/머지 전 `./gradlew build -x test` 필수.** PR #1이 빌드 확인 없이 머지되어 `@Query`/`@Param` import 누락으로 main 컴파일이 깨졌다(0428cf0). CI 없음 — TODO: CI 설정 여부 확인.
- 엔드포인트를 추가하면 (1) `SecurityConfig` 접근 규칙, (2) 필요 시 `@RateLimited`를 함께 확인.
- MinIO 이미지는 더 이상 배포되지 않아 compose는 `rustfs/rustfs`로 대체했다(컨테이너명은 `bookcover-minio` 유지). ES는 nori 없으면 인덱스 설정(`resources/elasticsearch/book-covers-settings.json`)이 실패한다.
- IP 단위 레이트 리밋은 `getRemoteAddr()` 기준이다. prod는 `server.forward-headers-strategy: native`로 사설망 프록시의 X-Forwarded-For를 신뢰한다. 프록시가 공인 IP면 Tomcat `internal-proxies` 설정 필요.
- 로컬 RustFS에 `bookcovers` 버킷이 없으면 업로드가 500(NoSuchBucket). `aws --endpoint-url http://localhost:9000 s3 mb s3://bookcovers` (키 minioadmin/minioadmin123).
  버킷을 새로 만들면 이미지 읽기도 막혀 있어 브라우저(백오피스 등)에서 표지가 403으로 안 보인다. 백엔드는 정책을 설정하지 않으므로 객체 읽기만 공개한다:
  `aws --endpoint-url http://localhost:9000 s3api put-bucket-policy --bucket bookcovers --policy '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":"*","Action":["s3:GetObject"],"Resource":["arn:aws:s3:::bookcovers/*"]}]}'` (목록 조회는 계속 막힘)
- 매핑 안 된 경로도 404가 아니라 **500(INTERNAL_ERROR)** 이 나온다(`GlobalExceptionHandler`의 `@ExceptionHandler(Exception.class)`가 라우팅 실패까지 잡음). 500이면 경로 오타부터 의심.
- 브라우징은 `/search/browse/...`에 있다. `SecurityConfig`의 `GET /browse/**` permitAll 규칙은 매칭되는 컨트롤러가 없다.
- 처리되지 않은 예외는 `GlobalExceptionHandler`에서 로그로 남긴다 — 500 원인은 서버 로그부터 확인.
- 이미지 한도(800×1200, 10MB, 4천만 픽셀, 형식)는 `ImageValidator` **상수**다. `application.yml`의 `image.min-width/min-height/max-file-size/allowed-formats`는 읽히지 않으므로 yml만 바꾸면 아무 일도 안 일어난다.
- 책·리디자인에 넣는 이미지 URL은 업로드 API가 남긴 `image_assets` 기록이 있어야 한다(`requireVerifiedImage`). 스크립트·테스트에서 외부 URL을 그대로 넣으면 400.
- `image_assets`의 해시 3종은 의미가 다르다: `sha256`=픽셀(테이크다운 차단), `file_sha256`=파일(동일 탐지), `perceptual_hash`=dHash(유사 후보). 섞어 쓰지 말 것.
- 콘텐츠 이벤트는 outbox(`content_outbox_events`)를 거쳐 1초마다 1건씩 Kafka로 간다. 검색 반영이 늦으면 미발행 행부터 본다. 모더레이션 이벤트는 outbox를 쓰지 않는다.
- 공개 조회는 `PUBLISHED`만 노출해야 한다(`Media.isPublic()`, ES `status` 필터). ES 인덱스에는 모든 상태 문서가 들어 있으므로 새 검색 쿼리에 필터를 빠뜨리면 DRAFT·HIDDEN이 샌다.
- 테이크다운 승인은 콘텐츠를 HIDDEN으로 바꾸고 S3 파일을 **삭제**한다. `restore`로는 이미지가 돌아오지 않는다.
