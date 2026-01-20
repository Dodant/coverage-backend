# Implementation Plan: Book Cover Archive

## Overview

북커버 디자인 아카이브 백엔드 시스템을 Spring Boot 3.x (Kotlin)로 구현합니다. 도메인 모델부터 시작하여 서비스 레이어, API 컨트롤러, 인프라스트럭처 순으로 점진적으로 구현합니다.

## Tasks

- [ ] 1. 프로젝트 초기 설정
  - [x] 1.1 Spring Boot 프로젝트 생성 및 의존성 설정
    - Spring Boot 3.x, Kotlin, Spring Data JPA, Spring Security, PostgreSQL, Redis, Elasticsearch 의존성 추가
    - Kotest 테스트 프레임워크 설정
    - _Requirements: 21.1_
  - [ ] 1.2 Docker Compose 개발 환경 설정
    - PostgreSQL, Redis, Elasticsearch, MinIO 컨테이너 구성
    - _Requirements: 21.1_
  - [ ] 1.3 애플리케이션 설정 파일 구성
    - application.yml 프로파일별 설정 (dev, test, prod)
    - _Requirements: 21.1_

- [ ] 2. 도메인 모델 구현 - 기본 엔티티
  - [ ] 2.1 공통 엔티티 및 Enum 구현
    - ContentStatus, BaseEntity 구현
    - _Requirements: 21.2_
  - [ ] 2.2 Media 추상 클래스 구현
    - 확장 가능한 미디어 베이스 클래스
    - _Requirements: 21.1, 21.2_
  - [ ] 2.3 Author, Publisher, Designer, Translator 엔티티 구현
    - 별칭(alias) 지원 포함
    - _Requirements: 10.4, 19.6_
  - [ ] 2.4 Work 엔티티 구현
    - 같은 작품 그룹화를 위한 엔티티
    - _Requirements: 6.1_
  - [ ]* 2.5 Property 테스트: 데이터 정규화
    - **Property 13: 데이터 정규화**
    - **Validates: Requirements 13.1, 13.2, 13.3**

- [ ] 3. 도메인 모델 구현 - 책 관련 엔티티
  - [ ] 3.1 BookCover 엔티티 구현
    - 이미지 URL, 썸네일, 메타데이터 포함
    - _Requirements: 1.4, 14.8_
  - [ ] 3.2 OriginalBook 엔티티 구현
    - Media 상속, 원서 고유 필드 포함
    - _Requirements: 1.1, 1.5, 1.6, 1.7_
  - [ ] 3.3 TranslatedBook 엔티티 구현
    - Media 상속, 원서 연결 포함
    - _Requirements: 2.1, 2.2, 2.5_
  - [ ] 3.4 Redesign 엔티티 구현
    - 원서/번역서 연결, 회원 연결 포함
    - _Requirements: 3.1, 3.7_
  - [ ]* 3.5 Property 테스트: 원서 등록 라운드트립
    - **Property 3: 원서 등록 라운드트립**
    - **Validates: Requirements 1.5**

- [ ] 4. 도메인 모델 구현 - 회원 및 소셜
  - [ ] 4.1 User 엔티티 구현
    - 역할, 칭호, SNS 정보 포함
    - _Requirements: 7.1, 7.2, 7.3, 7.4_
  - [ ] 4.2 Like 엔티티 구현
    - 다형성 좋아요 (커버, 리디자인)
    - _Requirements: 4.1, 4.3_
  - [ ] 4.3 Comment 엔티티 구현
    - 다형성 댓글
    - _Requirements: 5.1, 5.3_
  - [ ] 4.4 Collection, CollectionItem 엔티티 구현
    - 공개/비공개, 공유 토큰 포함
    - _Requirements: 11.1, 11.9, 11.10_
  - [ ]* 4.5 Property 테스트: 좋아요 토글
    - **Property 5: 좋아요 토글**
    - **Validates: Requirements 4.1, 4.2**

- [ ] 5. 도메인 모델 구현 - 관리 기능
  - [ ] 5.1 DesignTag 엔티티 구현
    - 카테고리, 승인 상태 포함
    - _Requirements: 20.1, 20.4_
  - [ ] 5.2 Report 엔티티 구현
    - 신고 사유, 처리 상태 포함
    - _Requirements: 15.2, 15.3_
  - [ ] 5.3 AuditLog 엔티티 구현
    - 변경 전/후 값 JSON 저장
    - _Requirements: 16.1, 16.6_
  - [ ] 5.4 TakedownRequest 엔티티 구현
    - 저작권 삭제 요청
    - _Requirements: 17.1, 17.2_
  - [ ]* 5.5 Property 테스트: 감사 로그 기록
    - **Property 16: 감사 로그 기록**
    - **Validates: Requirements 16.1**

- [ ] 6. Checkpoint - 도메인 모델 검증
  - 모든 엔티티 테스트 통과 확인
  - 데이터베이스 스키마 생성 확인

- [ ] 7. Repository 레이어 구현
  - [ ] 7.1 기본 Repository 인터페이스 구현
    - OriginalBookRepository, TranslatedBookRepository, RedesignRepository
    - _Requirements: 1.5, 2.1, 3.4_
  - [ ] 7.2 회원/소셜 Repository 구현
    - UserRepository, LikeRepository, CommentRepository, CollectionRepository
    - _Requirements: 4.1, 5.1, 11.2_
  - [ ] 7.3 관리 Repository 구현
    - ReportRepository, AuditLogRepository, DesignTagRepository
    - _Requirements: 15.3, 16.2, 20.1_
  - [ ] 7.4 커스텀 쿼리 메서드 구현
    - 중복 검사, 유사 항목 검색 쿼리
    - _Requirements: 12.1, 12.2_

- [ ] 8. 검증 로직 구현
  - [ ] 8.1 ISBN 검증기 구현
    - ISBN-10, ISBN-13 형식 검증
    - _Requirements: 1.2, 2.3_
  - [ ] 8.2 이미지 검증기 구현
    - 해상도, 파일 형식, 파일 크기 검증
    - _Requirements: 14.1, 14.4, 14.5_
  - [ ] 8.3 URL 검증기 구현
    - 웹사이트, 포트폴리오 URL 형식 검증
    - _Requirements: 7.2, 7.3_
  - [ ] 8.4 국가/언어 코드 검증기 구현
    - ISO 3166-1, ISO 639-1 검증
    - _Requirements: 13.1, 13.2_
  - [ ]* 8.5 Property 테스트: ISBN 형식 검증
    - **Property 2: ISBN 형식 검증**
    - **Validates: Requirements 1.2, 2.3**
  - [ ]* 8.6 Property 테스트: 이미지 검증
    - **Property 14: 이미지 검증**
    - **Validates: Requirements 14.1, 14.4, 14.5**

- [ ] 9. 서비스 레이어 구현 - 책 관리
  - [ ] 9.1 BookService 구현
    - 원서/번역서 CRUD, 중복 검사
    - _Requirements: 1.1, 1.5, 2.1, 12.1, 12.4_
  - [ ] 9.2 RedesignService 구현
    - 리디자인 CRUD, 회원 연결
    - _Requirements: 3.1, 3.2, 3.5, 3.6_
  - [ ] 9.3 컨트리뷰터 칭호 자동 부여 로직 구현
    - 등록 횟수 추적 및 칭호 변경
    - _Requirements: 7.5, 7.6_
  - [ ]* 9.4 Property 테스트: 원서 등록 입력 검증
    - **Property 1: 원서 등록 입력 검증**
    - **Validates: Requirements 1.1, 1.3**
  - [ ]* 9.5 Property 테스트: 번역서-원서 연결
    - **Property 4: 번역서-원서 연결**
    - **Validates: Requirements 2.1, 2.6**
  - [ ]* 9.6 Property 테스트: 컨트리뷰터 칭호 자동 부여
    - **Property 9: 컨트리뷰터 칭호 자동 부여**
    - **Validates: Requirements 7.5**

- [ ] 10. 서비스 레이어 구현 - 소셜 기능
  - [ ] 10.1 SocialService - 좋아요 기능 구현
    - 토글 좋아요, 카운트 관리
    - _Requirements: 4.1, 4.2, 4.4_
  - [ ] 10.2 SocialService - 댓글 기능 구현
    - 댓글 CRUD, 공백 검증
    - _Requirements: 5.1, 5.2, 5.4, 5.5_
  - [ ] 10.3 CollectionService 구현
    - 컬렉션 CRUD, 아이템 관리, 복제
    - _Requirements: 11.1, 11.2, 11.7, 11.11_
  - [ ]* 10.4 Property 테스트: 공백 댓글 거부
    - **Property 6: 공백 댓글 거부**
    - **Validates: Requirements 5.2**
  - [ ]* 10.5 Property 테스트: 댓글 삭제
    - **Property 7: 댓글 삭제**
    - **Validates: Requirements 5.4**
  - [ ]* 10.6 Property 테스트: 컬렉션 아이템 관리
    - **Property 11: 컬렉션 아이템 관리**
    - **Validates: Requirements 11.2, 11.7**

- [ ] 11. Checkpoint - 서비스 레이어 검증
  - 모든 서비스 테스트 통과 확인
  - Property 테스트 100회 반복 통과 확인

- [ ] 12. 인프라스트럭처 구현 - 이미지 처리
  - [ ] 12.1 ImageService 구현
    - S3/MinIO 업로드, 썸네일 생성
    - _Requirements: 14.7, 14.8_
  - [ ] 12.2 이미지 색상 추출 구현
    - 대표 색상 추출 로직
    - _Requirements: 20.2_
  - [ ] 12.3 이미지 sRGB 변환 구현
    - 색공간 표준화
    - _Requirements: 14.7_

- [ ] 13. 인프라스트럭처 구현 - 검색
  - [ ] 13.1 Elasticsearch 인덱스 설정
    - 한국어 분석기 설정, 인덱스 매핑
    - _Requirements: 8.1_
  - [ ] 13.2 SearchService 구현
    - 전문 검색, 필터링, 정렬
    - _Requirements: 8.1, 8.2, 8.3_
  - [ ] 13.3 브라우징 기능 구현
    - 국가별, 연도별, 장르별, 태그별 브라우징
    - _Requirements: 8.5, 8.6, 8.7, 20.6_
  - [ ]* 13.4 Property 테스트: 검색 결과 관련성
    - **Property 10: 검색 결과 관련성**
    - **Validates: Requirements 8.1**

- [ ] 14. 인프라스트럭처 구현 - 레이트 리밋
  - [ ] 14.1 RateLimitService 구현
    - Redis 기반 레이트 리밋
    - _Requirements: 18.1, 18.5_
  - [ ] 14.2 레이트 리밋 인터셉터 구현
    - API 요청 레이트 리밋 적용
    - _Requirements: 18.2, 18.3_
  - [ ]* 14.3 Property 테스트: 레이트 리밋 적용
    - **Property 17: 레이트 리밋 적용**
    - **Validates: Requirements 18.1**

- [ ] 15. 서비스 레이어 구현 - 관리 기능
  - [ ] 15.1 ModerationService - 신고 처리 구현
    - 신고 접수, 자동 비공개 처리
    - _Requirements: 15.2, 15.3, 15.4_
  - [ ] 15.2 ModerationService - 콘텐츠 관리 구현
    - 숨김, 복원, 삭제 처리
    - _Requirements: 15.5, 15.6, 15.7_
  - [ ] 15.3 AuditService 구현
    - 감사 로그 기록, 조회
    - _Requirements: 16.1, 16.2, 16.5_
  - [ ] 15.4 TakedownService 구현
    - 저작권 삭제 요청 처리
    - _Requirements: 17.1, 17.3, 17.4_
  - [ ]* 15.5 Property 테스트: 자동 비공개 처리
    - **Property 15: 자동 비공개 처리**
    - **Validates: Requirements 15.4**
  - [ ]* 15.6 Property 테스트: ISBN 중복 감지
    - **Property 12: ISBN 중복 감지**
    - **Validates: Requirements 12.1**
  - [ ]* 15.7 Property 테스트: 에디션 그룹화
    - **Property 8: 에디션 그룹화**
    - **Validates: Requirements 6.1**

- [ ] 16. Checkpoint - 인프라스트럭처 검증
  - 이미지 업로드/처리 테스트
  - Elasticsearch 검색 테스트
  - Redis 레이트 리밋 테스트


- [ ] 17. API 컨트롤러 구현 - 인증
  - [ ] 17.1 Spring Security 설정
    - JWT 인증, 역할 기반 권한
    - _Requirements: 7.1_
  - [ ] 17.2 AuthController 구현
    - 회원가입, 로그인, 토큰 갱신
    - _Requirements: 7.1, 7.8_
  - [ ] 17.3 UserController 구현
    - 프로필 조회, 수정
    - _Requirements: 7.7, 7.8_

- [ ] 18. API 컨트롤러 구현 - 책 관리
  - [ ] 18.1 OriginalBookController 구현
    - 원서 CRUD API
    - _Requirements: 1.1, 1.5_
  - [ ] 18.2 TranslatedBookController 구현
    - 번역서 CRUD API
    - _Requirements: 2.1, 2.6_
  - [ ] 18.3 RedesignController 구현
    - 리디자인 CRUD API
    - _Requirements: 3.1, 3.4_
  - [ ] 18.4 CoverController 구현
    - 커버 이미지 업로드, 조회
    - _Requirements: 14.1, 14.8_

- [ ] 19. API 컨트롤러 구현 - 소셜/검색
  - [ ] 19.1 LikeController 구현
    - 좋아요 토글 API
    - _Requirements: 4.1, 4.2_
  - [ ] 19.2 CommentController 구현
    - 댓글 CRUD API
    - _Requirements: 5.1, 5.4_
  - [ ] 19.3 CollectionController 구현
    - 컬렉션 CRUD, 공유 API
    - _Requirements: 11.1, 11.9, 11.11_
  - [ ] 19.4 SearchController 구현
    - 검색, 필터링, 브라우징 API
    - _Requirements: 8.1, 8.5, 8.6, 8.7_

- [ ] 20. API 컨트롤러 구현 - 엔티티 조회
  - [ ] 20.1 AuthorController 구현
    - 작가 프로필, 작품 목록 API
    - _Requirements: 10.2, 10.4_
  - [ ] 20.2 PublisherController 구현
    - 출판사 프로필, 출간 목록 API
    - _Requirements: 10.1, 10.4_
  - [ ] 20.3 DesignerController 구현
    - 디자이너 프로필, 작품 목록 API
    - _Requirements: 10.3, 10.4_
  - [ ] 20.4 TagController 구현
    - 태그 목록, 태그별 브라우징 API
    - _Requirements: 20.3, 20.6_

- [ ] 21. 관리자 API 구현
  - [ ] 21.1 AdminReportController 구현
    - 신고 큐 조회, 처리 API
    - _Requirements: 19.2_
  - [ ] 21.2 AdminModerationController 구현
    - 콘텐츠 관리, 사용자 정지 API
    - _Requirements: 15.5, 18.6_
  - [ ] 21.3 AdminEntityController 구현
    - 엔티티 병합, 별칭 관리 API
    - _Requirements: 19.5, 19.6_
  - [ ] 21.4 AdminDashboardController 구현
    - 통계, 로그 조회 API
    - _Requirements: 19.7, 19.8, 19.9_
  - [ ] 21.5 AdminTakedownController 구현
    - 저작권 삭제 요청 처리 API
    - _Requirements: 19.4_

- [ ] 22. Checkpoint - API 통합 테스트
  - 모든 API 엔드포인트 통합 테스트
  - 인증/권한 테스트
  - 에러 응답 형식 검증

- [ ] 23. 에러 처리 및 예외 핸들링
  - [ ] 23.1 GlobalExceptionHandler 구현
    - 표준 에러 응답 형식
    - _Requirements: 1.3, 14.2, 14.6_
  - [ ] 23.2 커스텀 예외 클래스 구현
    - ValidationException, DuplicateIsbnException, RateLimitExceededException
    - _Requirements: 12.1, 18.5_
  - [ ] 23.3 에러 코드 상수 정의
    - 에러 코드 체계 구현
    - _Requirements: 8.4_

- [ ] 24. 비동기 처리 구현
  - [ ] 24.1 Kafka 설정 및 이벤트 발행
    - 콘텐츠 등록/수정 이벤트
    - _Requirements: 21.1_
  - [ ] 24.2 검색 인덱스 동기화 리스너
    - Elasticsearch 인덱스 자동 업데이트
    - _Requirements: 19.7_
  - [ ] 24.3 알림 이벤트 리스너
    - 신고 자동 비공개 알림
    - _Requirements: 15.4_

- [ ] 25. 최종 통합 및 검증
  - [ ] 25.1 전체 Property 테스트 실행
    - 17개 Property 테스트 100회 반복 실행
    - _Requirements: All_
  - [ ] 25.2 API 문서화
    - OpenAPI/Swagger 문서 생성
    - _Requirements: 21.1_
  - [ ] 25.3 성능 테스트
    - 검색, 이미지 업로드 성능 검증
    - _Requirements: 14.8_

- [ ] 26. Final Checkpoint - 전체 시스템 검증
  - 모든 테스트 통과 확인
  - API 문서 검토
  - 코드 품질 검토

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties (17개)
- Unit tests validate specific examples and edge cases
- Kotest Property Testing 라이브러리 사용, 최소 100회 반복 실행
