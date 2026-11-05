# 알려진 결함과 한계

코드를 읽고 확인한 동작 중 의도와 다르거나 수정 시 주의할 것들. **대부분 런타임 재현은 하지 않았다** — 고치기 전에 재현부터 할 것. 고치면 이 목록에서 지운다. 의도적인 단순화는 코드의 `// ponytail:` 주석에 있다(`grep -rn "ponytail:" src/main`).

## 권한·노출

| 항목 | 위치 | 비고 |
|---|---|---|
| `GET /redesigns/my`가 공개 GET 패턴(`/redesigns/**`)에 걸려 비로그인 시 NPE → 500 | `RedesignController.getMyRedesigns`, `SecurityConfig` | 401이 맞다 |
| 리디자인 단건은 HIDDEN도 노출, 목록(`by-original/translated/user`)은 상태 필터 없음(DELETED 포함) | `RedesignServiceImpl.getRedesign`, `getRedesignsBy*` | `RedesignRepository`에 `...AndStatus` 메서드가 있으나 미사용 |
| `GET /covers/{id}`는 소속 책이 DRAFT/HIDDEN이어도 표지를 보여준다 | `CoverController` | `cover.hidden`만 확인 |
| 저자·출판사 프로필의 책 수는 모든 상태를 센다(목록은 PUBLISHED만) | `OriginalBookRepository.countByAuthorId/countByPublisherId` | 숫자 불일치 |
| `GET /tags?category=`, `GET /tags/{id}`는 미승인 태그도 반환 | `TagController` | 다른 분기는 approved만 |
| 책 삭제·리디자인 수정/삭제에 소유권 검사 없음 | `BookServiceImpl`, `RedesignServiceImpl` | 책 **편집**은 위키 모델로 의도됨. **삭제·리디자인까지 의도인지는 미확인** |
| 표지 추가·대표 표지 변경이 DRAFT/HIDDEN 책에도 된다 | `BookServiceImpl.findActiveBook`(DELETED만 제외) | |
| 읽기 GET 중 로그인이 필요한 것: 댓글 목록, 좋아요 수, `/users/{id}`, `/users/search`, `/collections/{id}` | `SecurityConfig` | 의도 미확인. `/collections/{id}`의 비로그인 분기는 도달 불가, LINK_ONLY 컬렉션은 ID만 알면 로그인 사용자 누구나 조회 |
| 서비스의 ADMIN 재검증 실패가 403이 아니라 400 | Catalog/Relationship/ImageInspection/CoverGroup/Tag 서비스 | `ValidationException` 사용 |
| `ModerationServiceImpl`의 hide/restore/delete/suspend는 서비스 재검증 없음 | | `/admin/**` URL 규칙에만 의존 |
| `SecurityConfig`의 `GET /browse/**`는 매칭되는 컨트롤러가 없다 | | 브라우징은 `/search/browse/**` |
| 매핑 안 된 경로가 404가 아니라 500 | `GlobalExceptionHandler`의 `Exception` 핸들러 | |

## 이벤트·검색

| 항목 | 위치 | 비고 |
|---|---|---|
| outbox 릴레이는 tick당 1건(≈1건/초). 대량 변경 시 색인이 밀린다 | `ContentOutboxRelay` | |
| 실패한 outbox 이벤트가 큐 맨 앞에 남아 무한 재시도하며 뒤 이벤트를 막는다(시도 횟수 없음) | `ContentOutboxRelay` | `SKIP LOCKED`도 없어 다중 인스턴스는 같은 행에서 직렬화 |
| 발행된 outbox 행을 지우지 않는다 | | 테이블이 계속 커진다 |
| `KafkaEventPublisher`의 "outbox로 바꿀 것" `ponytail:` 주석은 콘텐츠 이벤트에 대해선 이미 해결됨(모더레이션 이벤트만 해당) | `KafkaEventPublisher.java` | |
| 저자·출판사 병합은 재색인 이벤트를 내지 않아 ES의 저자/출판사명이 낡는다(디자이너 병합은 냄) | `EntityManagementServiceImpl` | |
| `BookService.linkToWork`는 호출처 없는 죽은 코드(감사·이벤트도 없음). 작품 연결은 관계 수정 API를 쓴다 | `BookServiceImpl` | |
| 모더레이션 알림 미구현(리스너는 로그만), `CONTENT_HIDDEN` 등 다수 이벤트 타입은 정의만 있음 | `ModerationEventListener` | |
| 검색 `facets`가 항상 빈 값 | `SearchServiceImpl` | |
| ES `designers`는 숨김 표지 크레딧도 포함(태그는 숨김 제외) | `SearchServiceImpl` | |
| `BrowsingServiceImpl`의 나라·연도·장르 메서드는 호출처 없음(`SearchServiceImpl`과 중복) | | |
| 재색인은 동기·단일 트랜잭션, 도중 검색이 빈다 | `AdminSearchController` | `ponytail:` 주석 있음 |

## 이미지

| 항목 | 위치 | 비고 |
|---|---|---|
| `application.yml`의 `image.min-width/min-height/max-file-size/allowed-formats`는 읽히지 않는다 | `ImageValidator` 상수 | 값을 바꾸려면 코드 수정. `CatalogServiceImpl`도 800×1200을 따로 하드코딩 |
| WebP는 형식 검사는 통과하지만 JDK ImageIO에 WebP 리더가 없어 실제 업로드는 실패할 가능성이 높다 | `build.gradle`에 플러그인 없음 | 추론, 미실행 |
| 썸네일은 너비 300 비율 유지(`FIT_TO_WIDTH`). `thumbnail.height`는 무시 | `ImageServiceImpl` | |
| `dominantColor`, `layout`을 채우는 코드가 없다(`extractDominantColor` 호출처 없음) | | |
| 업로드: S3 PUT 후 DB 저장 실패 시 S3 객체가 고아로 남는다 | `ImageServiceImpl.uploadImage` | |
| 차단: DB 트랜잭션 안에서 S3 삭제. S3 오류 시 승인은 롤백되지만 일부 파일은 이미 삭제됐을 수 있다 | `ImageServiceImpl.blockImage` | `ponytail:` 주석 있음 |
| 이미지 매칭은 요청마다 전체 표지 쌍 비교(O(n²), 전부 메모리 적재) | `ImageInspectionServiceImpl` | 대규모 시 후보 인덱스/백그라운드 잡 |
| `restore`는 테이크다운된 책을 PUBLISHED로 되돌리지만 표지 숨김·이미지 차단은 그대로(파일도 삭제됨) | `ModerationServiceImpl.restoreContent` | |

## 데이터

| 항목 | 위치 | 비고 |
|---|---|---|
| 기여자 이름·시리즈 (name, publisher) unique 제약 없음 → 동시 등록 시 중복 | `BookServiceImpl` | `ponytail:` 주석, 관리자 병합으로 정리 |
| `CollectionItem` 중복 제약 없음 | | |
| DRAFT로 만든 원서는 나중에 공개돼도 기여 수에 안 들어간다. 표지 변형 추가도 기여 1회로 센다 | `BookServiceImpl`, `ContributionServiceImpl` | |
| 병합 감사 로그 JSON을 `String.format`으로 만들어 이름에 따옴표가 있으면 깨진다 | `EntityManagementServiceImpl` | |
| 댓글 삭제는 하드 삭제 | `SocialServiceImpl` | |
| `TakedownRequest.preventReupload`는 항상 false, `isReuploadPrevented`는 죽은 경로 | `TakedownServiceImpl` | 차단은 `image_assets.sha256` |
| `AuditEntityType`의 REDESIGN/USER/COLLECTION은 미사용 | | |
| 인덱스·CHECK 일부가 마이그레이션 SQL에만 있어 dev DB에 없다 | [DATA_MODEL.md](DATA_MODEL.md#스키마-관리-주의) | |

## 테스트 공백

테스트 없음: `ContentOutboxRelay`, `TakedownServiceImpl`, `ImageServiceImpl.blockImage`, `SocialServiceImpl`, `RateLimitServiceImpl`(Lua), `SearchServiceImpl`. Testcontainers 의존성과 `application-test.yml`은 있으나 쓰는 테스트가 없다. CI 없음.
