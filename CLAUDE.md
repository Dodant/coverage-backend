# CLAUDE.md

북커버 디자인 아카이브(원서·번역서 커버 비교, 리디자인, 소셜) REST API 백엔드.
Java 21 · Spring Boot 3.2.5 · Gradle · PostgreSQL · Redis · Elasticsearch(nori) · Kafka · S3 호환 스토리지(RustFS) · JWT(jjwt 0.12)

요구사항 원문: `.kiro/specs/book-cover-archive/{requirements,design,tasks}.md` — 코드·커밋의 "요구사항 N.M"은 여기 번호다.

## 명령어

```bash
docker compose up -d                 # Postgres/Redis/ES/RustFS/Kafka (ES는 첫 기동 시 nori 플러그인 설치)
./gradlew bootRun                    # 기본 프로필 dev, http://localhost:8080/api (context-path /api)
./gradlew build -x test              # 컴파일+패키징
./gradlew test                       # 전체 테스트
./gradlew test --tests 'com.bookcover.archive.validation.IsbnValidatorTest'   # 단일 테스트
python3 scripts/smoke_rate_limits.py # 실행 중인 서버 대상 시나리오 검증 (smoke_*.py 3종, 실패 시 non-zero)
```

- 헬스체크: `curl localhost:8080/api/actuator/health`, Swagger: `/api/swagger-ui.html`
- 린트/포맷: TODO — Checkstyle/Spotless 등 설정 없음
- 사람용 실행 가이드(계정 생성, ADMIN 승격, API 개요, prod 환경변수)는 `README.md`
- 도메인 모델·Kafka 흐름·검색 인덱싱·알려진 결함은 `ARCHITECTURE.md`

## 설정·환경변수

- `application.yml`(공통) + `application-{dev,test,prod}.yml`. `spring.profiles.active: dev`가 기본값.
- dev는 접속 정보가 yml에 하드코딩(docker-compose 값과 일치). `.env`/`.env.example` 없음. `application-local.yml`은 gitignore 대상.
- prod는 전부 환경변수: `DATABASE_URL/USERNAME/PASSWORD`, `REDIS_HOST/PORT/PASSWORD`, `ELASTICSEARCH_URIS`, `KAFKA_BOOTSTRAP_SERVERS`, `S3_*`, `JWT_SECRET`(필수), `JWT_EXPIRATION`, `JWT_REFRESH_EXPIRATION`.
- 레이트 리밋 한도는 `application.yml`의 `rate-limit.{upload,comment,like,report,takedown}`.
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
- **principal은 `CustomUserDetails`(String/UUID 아님).** 사용자 ID는 다음 두 가지로만 얻는다:
  - 컨트롤러 파라미터: `@AuthenticationPrincipal CustomUserDetails userDetails` → `userDetails.getId()`
  - 그 외(인터셉터 등): `SecurityUtils.getCurrentUserId()` (`Optional<UUID>`)
- 과거 인터셉터가 principal을 String으로 가정해 모든 요청을 비로그인으로 처리한 버그가 있었다(eb1e9be). 직접 파싱하지 말 것.
- 접근 규칙은 `SecurityConfig` 한 곳: 공개 GET 목록, `/admin/**`는 ADMIN, `POST /takedowns`는 비회원 허용, 나머지는 인증 필요. 새 공개 엔드포인트는 여기에 추가해야 한다.
- 서비스 레이어에서도 ADMIN 역할을 재검증한다(`ModerationServiceImpl`, `TakedownServiceImpl`).

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

- 현재 `src/test`는 `validation/` 단위 테스트 5개 + `RateLimitInterceptorTest`(Mockito)뿐. Spring 컨텍스트를 띄우는 테스트 없음.
- Testcontainers 의존성과 `application-test.yml`(포트 5433/6380/9201/9093/9002)은 있지만 이를 쓰는 통합 테스트도, 해당 포트를 띄우는 compose도 없다. TODO: 통합 테스트 실행 방법 미정.
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
- 매핑 안 된 경로도 404가 아니라 **500(INTERNAL_ERROR)** 이 나온다(`GlobalExceptionHandler`의 `@ExceptionHandler(Exception.class)`가 라우팅 실패까지 잡음). 500이면 경로 오타부터 의심.
- 브라우징은 `/search/browse/...`에 있다. `SecurityConfig`의 `GET /browse/**` permitAll 규칙은 매칭되는 컨트롤러가 없다.
- 처리되지 않은 예외는 `GlobalExceptionHandler`에서 로그로 남긴다 — 500 원인은 서버 로그부터 확인.
