# 개발·테스트·배포 절차

로컬 실행·계정 생성·prod 환경변수 표는 [README.md](../README.md)에 있다(중복하지 않음). 이 문서는 **변경 유형별 체크리스트**, 테스트 방법, 배포 순서를 다룬다. 코드 규칙은 [CLAUDE.md](../CLAUDE.md).

## 로컬 환경 준비

```bash
docker compose up -d          # postgres 5432, redis 6379, ES 9200(첫 기동 시 nori 설치로 느림), RustFS 9000/9001, kafka 9092
docker compose ps             # elasticsearch가 healthy인지 확인 후 기동
./gradlew bootRun             # dev 프로필, ddl-auto=update로 스키마 자동 생성
```

- 업로드가 500(NoSuchBucket)이면 버킷 생성과 공개 읽기 정책이 필요하다 — 명령은 [CLAUDE.md](../CLAUDE.md#주의사항-실제로-겪은-것) 참고.
- 에이전트(register-book) 작업 시 `RATE_LIMIT_EXEMPT_USER_IDS`, `IMAGE_RESOLUTION_EXEMPT_USER_IDS`에 에이전트 계정 UUID를 넣고 bootRun.
- `application-local.yml`은 gitignore 대상이라 개인 설정에 쓸 수 있다(`--args='--spring.profiles.active=dev,local'`).

## 변경 유형별 체크리스트

### 엔드포인트 추가·변경
1. 접근 규칙: `config/security/SecurityConfig.java`. 공개 GET이면 패턴 추가. 공개 패턴 아래 경로라도 로그인이 필요한 핸들러는 `userDetails` null을 직접 막아야 한다(`/redesigns/my` 결함 참고).
2. 관리자 기능은 `controller/admin/`에 두고 클래스에 `@PreAuthorize("hasRole('ADMIN')")`.
3. 쓰기/남용 가능 엔드포인트면 `@RateLimited(action = RateLimitAction.X)`. 새 액션은 `service/RateLimitAction` enum + `config/RateLimitConfig` 필드·getter/setter + `RateLimitServiceImpl`의 `switch` + `application.yml`의 `rate-limit.*` 네 곳.
4. 로그인 사용자 ID는 `@AuthenticationPrincipal CustomUserDetails` → `getId()`, 컨트롤러 밖은 `SecurityUtils.getCurrentUserId()`.
5. 공개 조회면 상태 필터(`isPublic()` / `status = PUBLISHED`)를 넣는다.
6. 새 에러 종류는 `exception/ErrorCode`.
7. [docs/API.md](API.md)의 해당 표를 갱신.

### 엔티티 필드·테이블 변경 (스키마 변경)
1. 엔티티 수정. dev는 `ddl-auto=update`로 컬럼과 엔티티에 선언한 인덱스(`@Table(indexes=...)`)를 만들지만 **컬럼 삭제·타입 변경·데이터 이전은 하지 않고**, SQL에만 있는 부분 인덱스·CHECK·`ON DELETE CASCADE`도 만들지 않는다.
2. `scripts/migrations/<YYYY-MM-DD>_<이름>.sql`을 멱등하게 작성(`ADD COLUMN IF NOT EXISTS`, `CREATE INDEX IF NOT EXISTS`, 백필은 `WHERE ... IS NULL` 가드). 첫 줄 주석에 목적과 실행 시점("배포 전"/"배포 후")을 반드시 적는다.
3. 옛 앱이 쓰는 컬럼 삭제는 **배포 후** 실행하는 별도 스크립트로 분리(예: `2026-10-14_cover_credits_series.sql` → `2026-10-14_drop_book_designer.sql`).
4. 기존 행이 NULL일 때의 응답을 정한다(예: `variantType` getter가 NULL → `UNKNOWN`). 행이 있는 테이블에 기본값 없는 `NOT NULL` 컬럼을 추가하면 **마이그레이션 SQL 자체가 실패**하므로 nullable로 추가하거나 DEFAULT·백필을 넣는다. prod `validate`는 테이블·컬럼 존재와 타입만 보고 NULL 허용 여부는 검사하지 않는다(엔티티 `nullable=false`와 DB가 어긋나도 통과).
5. 응답 DTO·요청 DTO·[docs/DATA_MODEL.md](DATA_MODEL.md) 갱신.

### 콘텐츠 변경이 검색에 반영돼야 할 때
- 서비스 트랜잭션 안에서 `eventPublisher.publishContentEvent(new ContentEvent(타입, id, userId))`. outbox에 저장되고 릴레이가 Kafka로 보낸다. ES 문서는 리스너가 DB에서 다시 읽어 만들므로 이벤트에 데이터를 실을 필요 없다.
- ES 문서 내용은 `SearchServiceImpl`의 문서 변환 코드에서 결정된다.

### Elasticsearch 필드 추가
1. `search/document/BookCoverDocument`에 필드 + `@Field` 매핑.
2. 기존 인덱스에는 매핑이 자동으로 안 들어간다(동적 매핑 = standard 분석기). **배포 전에** 매핑을 먼저 넣는다:
   ```bash
   curl -XPUT "$ELASTICSEARCH_URIS/book_covers/_mapping" -H 'Content-Type: application/json' \
     -d '{"properties":{"<필드>":{"type":"keyword"}}}'
   # 한국어 텍스트면 {"type":"text","analyzer":"korean_analyzer","search_analyzer":"korean_analyzer"}
   ```
3. 배포 후 기존 문서를 채우려면 ADMIN 토큰으로 `POST /api/admin/search/reindex`(동기, 도중 검색 결과가 빔).
4. 로컬은 인덱스를 지우고 재기동하면 settings JSON과 매핑으로 새로 만든다.

### 이미지 처리 변경
- 업로드 한도는 `validation/ImageValidator` 상수다(`application.yml`의 `image.min-*`/`max-file-size`는 안 읽힘). `MAX_PIXELS`는 `ImageFingerprint`·이미지 검사와 공유한다.
- 책·리디자인이 받는 이미지 URL은 `image_assets` 검증 기록이 있어야 한다. 테스트·스크립트에서 임의 URL을 넣으면 400.
- 해시 3종(`sha256`=픽셀/차단, `file_sha256`=파일/동일, `perceptual_hash`=유사)의 의미를 바꾸지 않는다.

### 동시성이 걸린 변경
- 카운터는 `@Modifying @Query` 단일 UPDATE.
- 같은 대상의 경쟁을 막을 때는 기존 패턴을 따른다: 신고는 대상 행 `findByIdForUpdate`, 관계 수정은 ID 정렬 후 `PESSIMISTIC_WRITE` + `em.refresh` + 기대값 재검사.

## 테스트

```bash
./gradlew test                                                    # 전체 (docker 불필요: Mockito·순수 JUnit·H2)
./gradlew test --tests '*CatalogServiceImplTest'                  # 클래스 하나
./gradlew test --tests 'com.bookcover.archive.validation.IsbnValidatorTest'
./gradlew build -x test                                           # 커밋 전 컴파일 확인 (CI 없음)
```

| 종류 | 예 | 방식 |
|---|---|---|
| 순수 단위 | `validation/*Test`, `ImageFingerprintTest`, `SeriesOrderTest`, `CopyrightMetadataValidationTest` | JUnit, 목 없음 |
| 서비스 단위 | `BookServiceImplTest`, `ModerationServiceImplTest`, `ImageUploadLimitsTest`, `RateLimitInterceptorTest`, `JwtAuthenticationFilterTest` … | Mockito로 직접 생성(Spring 컨텍스트 없음, 생성자 주입이라 가능) |
| JPA 슬라이스 | `CatalogServiceImplTest`, `RelationshipServiceConcurrencyTest` | `@DataJpaTest` + H2 PostgreSQL 모드(속성은 클래스에 인라인) |

H2 슬라이스 테스트 작성 시 `CatalogServiceImplTest` 상단 설정을 복사한다:

```java
@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:<이름>;MODE=PostgreSQL;NON_KEYWORDS=YEAR,VALUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
```

- 서비스 협력자(감사·이벤트·ImageService)는 Mockito 목으로 넣는다. 동시성 테스트는 `@Transactional(propagation = NOT_SUPPORTED)` + `TransactionTemplate` 두 스레드(`RelationshipServiceConcurrencyTest`).
- 네이티브 SQL은 H2 PostgreSQL 모드와 실제 PostgreSQL 양쪽에서 돌아야 한다.
- `@SpringBootTest`/Testcontainers 테스트는 없다. `application-test.yml`(포트 5433/6380/9201/9093/9002)은 쓰이지 않는다.
- 테스트 이미지는 `src/test/java/.../support/TestImages`.

### 스모크 테스트 (실제 서버 대상)

서버 실행 + `docker compose` 필요. 실패 시 non-zero 종료.

```bash
python3 scripts/smoke_auth.py              # 토큰 종류 분리·갱신 회전·로그아웃·탈퇴
python3 scripts/smoke_report_takedown.py   # 신고·테이크다운
python3 scripts/smoke_rate_limits.py       # 레이트 리밋 (docker exec bookcover-redis로 rate_limit:* 초기화)
python3 scripts/smoke_cover_variants.py    # 표지 변형 (ES 색인 폴링)
python3 scripts/smoke_credits_series.py    # 크레딧·시리즈
BASE=http://localhost:8081/api python3 scripts/smoke_auth.py   # BASE는 smoke_auth·smoke_credits_series만 지원
```

## 배포 (prod)

배포 자동화·CI는 저장소에 없다. 확인된 절차:

1. `./gradlew build` (테스트 포함) → `build/libs/*.jar`.
2. 환경변수 준비: [README.md](../README.md#production-environment-variables-prod-profile). `JWT_SECRET` 필수.
3. **배포 전**: 아직 적용 안 된 `scripts/migrations/*.sql`을 날짜순으로 전부 실행(`psql ... < 파일`). 헤더에 **"배포 후"라고 적힌 스크립트만**(현재 `2026-10-14_drop_book_designer.sql`) 6단계로 미룬다. 라벨이 없는 스크립트(테이블·컬럼 추가)는 배포 전에 실행해야 하며, 빠뜨리면 `validate`에서 기동이 실패한다. 모두 멱등하지만 적용 이력 테이블이 없으므로 어디까지 적용했는지는 운영자가 따로 기록해야 한다.
4. ES 신규 필드가 있으면 `_mapping` PUT.
5. 앱 배포 (`SPRING_PROFILES_ACTIVE=prod`, `ddl-auto=validate` — 스키마가 엔티티와 안 맞으면 기동 실패).
6. **배포 후**: "배포 후" 스크립트(컬럼 삭제 등), 필요 시 `POST /admin/search/reindex`.
7. S3 버킷 정책: 백엔드는 정책을 설정하지 않는다. 표지를 브라우저에서 보이게 하려면 객체 읽기 공개가 필요하다.
8. 프록시가 공인 IP면 Tomcat `internal-proxies` 설정(아니면 IP 레이트 리밋이 프록시 IP 하나로 묶인다).

## 디버깅 팁

- 500이면 서버 로그의 `Unhandled exception`부터. 매핑 안 된 경로도 500이다(경로 오타 의심).
- 검색에 안 보이면: 책 상태가 PUBLISHED인지 → `content_outbox_events`에 `published_at IS NULL` 행이 쌓였는지 → Kafka DLT(`bookcover.content.events.DLT`) → 재색인.
- 401인데 토큰이 맞아 보이면: `typ`(리프레시 토큰을 액세스로 썼는지), 사용자 상태, 탈퇴 후 재가입(uid 불일치).
- 레이트 리밋 초기화: `docker exec bookcover-redis sh -c "redis-cli --scan --pattern 'rate_limit:*' | xargs -r redis-cli del"`.
