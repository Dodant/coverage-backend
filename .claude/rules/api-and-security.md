---
paths:
  - "src/main/java/com/bookcover/archive/controller/**"
  - "src/main/java/com/bookcover/archive/config/security/**"
  - "src/main/java/com/bookcover/archive/config/ratelimit/**"
---

# 컨트롤러·보안 변경 규칙

엔드포인트 목록과 현재 권한: `docs/API.md`. 체크리스트: `docs/DEVELOPMENT.md`의 "엔드포인트 추가·변경".

- 접근 규칙은 `SecurityConfig` 한 곳에서만 정한다. 공개 GET 패턴(`/books/**`, `/redesigns/**` 등) 아래에 로그인 필요 핸들러를 만들면 비로그인 시 `userDetails`가 null이다 — 경로를 옮기거나 명시적으로 401을 낸다.
- principal은 `CustomUserDetails`. 사용자 ID는 `@AuthenticationPrincipal CustomUserDetails` → `getId()` 또는 `SecurityUtils.getCurrentUserId()`. 직접 파싱 금지.
- 관리자 API는 `controller/admin/` + 클래스 `@PreAuthorize("hasRole('ADMIN')")`.
- 쓰기·남용 가능 엔드포인트는 `@RateLimited(action = ...)`을 붙여야 리밋이 걸린다(설정만으로는 동작 안 함).
- 요청 DTO는 `@Valid` + jakarta 제약, 메시지는 한국어. 기존 수정 요청에 선택 필드를 새로 추가할 때는 구형 클라이언트가 값을 지우지 않도록 "null=유지, 빈 문자열=비우기"로 한다(`copyrightHolder` 참고, 관리자 표지 PUT의 다른 필드는 전체 교체라 다름).
- 엔티티→DTO 변환은 컨트롤러에서 해도 된다(`open-in-view: true` 전제). 이 설정을 끄지 않는다.
- 변경 후 `docs/API.md`를 갱신한다.
