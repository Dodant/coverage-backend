---
paths:
  - "src/main/java/com/bookcover/archive/domain/**"
  - "src/main/java/com/bookcover/archive/search/**"
  - "src/main/java/com/bookcover/archive/repository/**"
  - "scripts/migrations/**"
---

# 엔티티·스키마·검색 변경 규칙

상세 절차: `docs/DEVELOPMENT.md`의 "엔티티 필드·테이블 변경", "Elasticsearch 필드 추가". 현재 모델: `docs/DATA_MODEL.md`.

- 엔티티 컬럼을 추가·변경하면 **같은 변경에** `scripts/migrations/<YYYY-MM-DD>_<이름>.sql`을 넣는다. 멱등(`IF NOT EXISTS`, NULL 가드 백필). prod는 `ddl-auto=validate`라 스크립트 없이 배포하면 기동이 실패한다.
- 컬럼 삭제는 배포 후 실행하는 별도 스크립트로 분리한다. dev `update`는 컬럼 삭제·타입 변경·데이터 이전을 하지 않고, SQL에만 있는 부분 인덱스·CHECK·`ON DELETE CASCADE`도 만들지 않는다(엔티티에 선언한 인덱스는 만든다).
- 스크립트 첫 줄 주석에 "배포 전"/"배포 후"를 적는다.
- 새 컬럼은 nullable로 추가하고, 기존 행의 NULL을 어떻게 응답할지 getter에서 정한다(예: `BookCover.getVariantType()` → UNKNOWN).
- 카운터는 `@Modifying @Query` 단일 UPDATE. 엔티티를 읽어 +1 하고 저장하지 않는다.
- 다형 대상(Like/Comment/CollectionItem/Report)은 FK가 없다. 대상 존재 검사는 서비스 몫이다.
- 표지 크레딧 수정은 `BookServiceImpl.applyCredits`의 diff 방식을 유지한다(전체 삭제 후 재삽입하면 unique 위반).
- ES 문서 필드를 추가하면 배포 전 `PUT book_covers/_mapping`이 필요하다(기존 인덱스엔 자동 반영 안 됨, 동적 매핑은 standard 분석기). 한국어 텍스트는 `korean_analyzer`.
- 새 검색·조회 쿼리는 `status = PUBLISHED` 필터를 넣는다. 인덱스에는 모든 상태 문서가 있다.
- 네이티브 SQL은 H2(PostgreSQL 모드) 테스트에서도 돌아야 한다.
- 변경 후 `docs/DATA_MODEL.md`를 갱신한다.
