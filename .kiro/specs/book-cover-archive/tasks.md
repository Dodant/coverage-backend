# Implementation Plan: Book Cover Archive

## Overview

북커버 디자인 아카이브 백엔드 시스템을 Spring Boot 3.x (Java 21)로 구현합니다. 도메인 모델부터 시작하여 서비스 레이어, API 컨트롤러, 인프라스트럭처 순으로 점진적으로 구현합니다.

## Tasks

- [x] 1. 프로젝트 초기 설정
  - [x] 1.1 Spring Boot 프로젝트 생성 및 의존성 설정
    - Spring Boot 3.x, Java 21, Spring Data JPA, Spring Security, PostgreSQL, Redis, Elasticsearch 의존성 추가
    - _Requirements: 21.1_
  - [x] 1.2 Docker Compose 개발 환경 설정
    - PostgreSQL, Redis, Elasticsearch, MinIO 컨테이너 구성
    - _Requirements: 21.1_
  - [x] 1.3 애플리케이션 설정 파일 구성
    - application.yml 프로파일별 설정 (dev, test, prod)
    - _Requirements: 21.1_

- [x] 2. 도메인 모델 구현 - 기본 엔티티
  - [x] 2.1 공통 엔티티 및 Enum 구현
    - ContentStatus, BaseEntity 구현
    - _Requirements: 21.2_
  - [x] 2.2 Media 추상 클래스 구현
    - 확장 가능한 미디어 베이스 클래스
    - _Requirements: 21.1, 21.2_
  - [x] 2.3 Author, Publisher, Designer, Translator 엔티티 구현
    - 별칭(alias) 지원 포함
    - _Requirements: 10.4, 19.6_
  - [x] 2.4 Work 엔티티 구현
    - 같은 작품 그룹화를 위한 엔티티
    - _Requirements: 6.1_

- [x] 3. 도메인 모델 구현 - 책 관련 엔티티
  - [x] 3.1 BookCover 엔티티 구현
    - 이미지 URL, 썸네일, 메타데이터 포함
    - _Requirements: 1.4, 14.8_
  - [x] 3.2 OriginalBook 엔티티 구현
    - Media 상속, 원서 고유 필드 포함
    - _Requirements: 1.1, 1.5, 1.6, 1.7_
  - [x] 3.3 TranslatedBook 엔티티 구현
    - Media 상속, 원서 연결 포함
    - _Requirements: 2.1, 2.2, 2.5_
  - [x] 3.4 Redesign 엔티티 구현
    - 원서/번역서 연결, 회원 연결 포함
    - _Requirements: 3.1, 3.7_

- [x] 4. 도메인 모델 구현 - 회원 및 소셜
  - [x] 4.1 User 엔티티 구현
    - 역할, 칭호, SNS 정보 포함
    - _Requirements: 7.1, 7.2, 7.3, 7.4_
  - [x] 4.2 Like 엔티티 구현
    - 다형성 좋아요 (커버, 리디자인)
    - _Requirements: 4.1, 4.3_
  - [x] 4.3 Comment 엔티티 구현
    - 다형성 댓글
    - _Requirements: 5.1, 5.3_
  - [x] 4.4 Collection, CollectionItem 엔티티 구현
    - 공개/비공개, 공유 토큰 포함
    - _Requirements: 11.1, 11.9, 11.10_

- [x] 5. 도메인 모델 구현 - 관리 기능
  - [x] 5.1 DesignTag 엔티티 구현
    - 카테고리, 승인 상태 포함
    - _Requirements: 20.1, 20.4_
  - [x] 5.2 Report 엔티티 구현
    - 신고 사유, 처리 상태 포함
    - _Requirements: 15.2, 15.3_
  - [x] 5.3 AuditLog 엔티티 구현
    - 변경 전/후 값 JSON 저장
    - _Requirements: 16.1, 16.6_
  - [x] 5.4 TakedownRequest 엔티티 구현
    - 저작권 삭제 요청
    - _Requirements: 17.1, 17.2_

- [x] 6. Repository 레이어 구현
  - [x] 6.1 기본 Repository 인터페이스 구현
    - OriginalBookRepository, TranslatedBookRepository, RedesignRepository
    - _Requirements: 1.5, 2.1, 3.4_
  - [x] 6.2 회원/소셜 Repository 구현
    - UserRepository, LikeRepository, CommentRepository, CollectionRepository
    - _Requirements: 4.1, 5.1, 11.2_
  - [x] 6.3 관리 Repository 구현
    - ReportRepository, AuditLogRepository, DesignTagRepository
    - _Requirements: 15.3, 16.2, 20.1_
  - [x] 6.4 커스텀 쿼리 메서드 구현
    - 중복 검사, 유사 항목 검색 쿼리
    - _Requirements: 12.1, 12.2_

- [x] 7. 검증 로직 구현
  - [x] 7.1 ISBN 검증기 구현
    - ISBN-10, ISBN-13 형식 검증
    - _Requirements: 1.2, 2.3_
  - [x] 7.2 이미지 검증기 구현
    - 해상도, 파일 형식, 파일 크기 검증
    - _Requirements: 14.1, 14.4, 14.5_
  - [x] 7.3 URL 검증기 구현
    - 웹사이트, 포트폴리오 URL 형식 검증
    - _Requirements: 7.2, 7.3_
  - [x] 7.4 국가/언어 코드 검증기 구현
    - ISO 3166-1, ISO 639-1 검증
    - _Requirements: 13.1, 13.2_

- [x] 8. 서비스 레이어 구현 - 책 관리
  - [x] 8.1 BookService 구현
    - 원서/번역서 CRUD, 중복 검사
    - _Requirements: 1.1, 1.5, 2.1, 12.1, 12.4_
  - [x] 8.2 RedesignService 구현
    - 리디자인 CRUD, 회원 연결
    - _Requirements: 3.1, 3.2, 3.5, 3.6_
  - [x] 8.3 컨트리뷰터 칭호 자동 부여 로직 구현
    - 등록 횟수 추적 및 칭호 변경
    - _Requirements: 7.5, 7.6_

- [x] 9. 서비스 레이어 구현 - 소셜 기능
  - [x] 9.1 SocialService - 좋아요 기능 구현
    - 토글 좋아요, 카운트 관리
    - _Requirements: 4.1, 4.2, 4.4_
  - [x] 9.2 SocialService - 댓글 기능 구현
    - 댓글 CRUD, 공백 검증
    - _Requirements: 5.1, 5.2, 5.4, 5.5_
  - [x] 9.3 CollectionService 구현
    - 컬렉션 CRUD, 아이템 관리, 복제
    - _Requirements: 11.1, 11.2, 11.7, 11.11_

- [x] 10. 인프라스트럭처 구현 - 이미지 처리
  - [x] 10.1 ImageService 구현
    - S3/MinIO 업로드, 썸네일 생성
    - _Requirements: 14.7, 14.8_
  - [x] 10.2 이미지 색상 추출 구현
    - 대표 색상 추출 로직
    - _Requirements: 20.2_
  - [x] 10.3 이미지 sRGB 변환 구현
    - 색공간 표준화
    - _Requirements: 14.7_

- [x] 11. 인프라스트럭처 구현 - 검색
  - [x] 11.1 Elasticsearch 인덱스 설정
    - 한국어 분석기 설정, 인덱스 매핑
    - _Requirements: 8.1_
  - [x] 11.2 SearchService 구현
    - 전문 검색, 필터링, 정렬
    - _Requirements: 8.1, 8.2, 8.3_
  - [x] 11.3 브라우징 기능 구현
    - 국가별, 연도별, 장르별, 태그별 브라우징
    - _Requirements: 8.5, 8.6, 8.7, 20.6_

- [x] 12. 인프라스트럭처 구현 - 레이트 리밋
  - [x] 12.1 RateLimitService 구현
    - Redis 기반 레이트 리밋
    - _Requirements: 18.1, 18.5_
  - [x] 12.2 레이트 리밋 인터셉터 구현
    - API 요청 레이트 리밋 적용
    - _Requirements: 18.2, 18.3_

- [x] 13. 서비스 레이어 구현 - 관리 기능
  - [x] 13.1 ModerationService - 신고 처리 구현
    - 신고 접수, 자동 비공개 처리
    - _Requirements: 15.2, 15.3, 15.4_
  - [x] 13.2 ModerationService - 콘텐츠 관리 구현
    - 숨김, 복원, 삭제 처리
    - _Requirements: 15.5, 15.6, 15.7_
  - [x] 13.3 AuditService 구현
    - 감사 로그 기록, 조회
    - _Requirements: 16.1, 16.2, 16.5_
  - [x] 13.4 TakedownService 구현
    - 저작권 삭제 요청 처리
    - _Requirements: 17.1, 17.3, 17.4_

- [x] 14. API 컨트롤러 구현 - 인증
  - [x] 14.1 Spring Security 설정
    - JWT 인증, 역할 기반 권한
    - _Requirements: 7.1_
  - [x] 14.2 AuthController 구현
    - 회원가입, 로그인, 토큰 갱신
    - _Requirements: 7.1, 7.8_
  - [x] 14.3 UserController 구현
    - 프로필 조회, 수정
    - _Requirements: 7.7, 7.8_

- [x] 15. API 컨트롤러 구현 - 책 관리
  - [x] 15.1 OriginalBookController 구현
    - 원서 CRUD API
    - _Requirements: 1.1, 1.5_
  - [x] 15.2 TranslatedBookController 구현
    - 번역서 CRUD API
    - _Requirements: 2.1, 2.6_
  - [x] 15.3 RedesignController 구현
    - 리디자인 CRUD API
    - _Requirements: 3.1, 3.4_
  - [x] 15.4 CoverController 구현
    - 커버 이미지 업로드, 조회
    - _Requirements: 14.1, 14.8_

- [ ] 16. API 컨트롤러 구현 - 소셜/검색
  - [ ] 16.1 LikeController 구현
    - 좋아요 토글 API
    - _Requirements: 4.1, 4.2_
  - [ ] 16.2 CommentController 구현
    - 댓글 CRUD API
    - _Requirements: 5.1, 5.4_
  - [ ] 16.3 CollectionController 구현
    - 컬렉션 CRUD, 공유 API
    - _Requirements: 11.1, 11.9, 11.11_
  - [ ] 16.4 SearchController 구현
    - 검색, 필터링, 브라우징 API
    - _Requirements: 8.1, 8.5, 8.6, 8.7_

- [ ] 17. API 컨트롤러 구현 - 엔티티 조회
  - [ ] 17.1 AuthorController 구현
    - 작가 프로필, 작품 목록 API
    - _Requirements: 10.2, 10.4_
  - [ ] 17.2 PublisherController 구현
    - 출판사 프로필, 출간 목록 API
    - _Requirements: 10.1, 10.4_
  - [ ] 17.3 DesignerController 구현
    - 디자이너 프로필, 작품 목록 API
    - _Requirements: 10.3, 10.4_
  - [ ] 17.4 TagController 구현
    - 태그 목록, 태그별 브라우징 API
    - _Requirements: 20.3, 20.6_

- [ ] 18. 관리자 API 구현
  - [ ] 18.1 AdminReportController 구현
    - 신고 큐 조회, 처리 API
    - _Requirements: 19.2_
  - [ ] 18.2 AdminModerationController 구현
    - 콘텐츠 관리, 사용자 정지 API
    - _Requirements: 15.5, 18.6_
  - [ ] 18.3 AdminEntityController 구현
    - 엔티티 병합, 별칭 관리 API
    - _Requirements: 19.5, 19.6_
  - [ ] 18.4 AdminDashboardController 구현
    - 통계, 로그 조회 API
    - _Requirements: 19.7, 19.8, 19.9_
  - [ ] 18.5 AdminTakedownController 구현
    - 저작권 삭제 요청 처리 API
    - _Requirements: 19.4_

- [ ] 19. 에러 처리 및 예외 핸들링
  - [ ] 19.1 GlobalExceptionHandler 구현
    - 표준 에러 응답 형식
    - _Requirements: 1.3, 14.2, 14.6_
  - [ ] 19.2 커스텀 예외 클래스 구현
    - ValidationException, DuplicateIsbnException, RateLimitExceededException
    - _Requirements: 12.1, 18.5_
  - [ ] 19.3 에러 코드 상수 정의
    - 에러 코드 체계 구현
    - _Requirements: 8.4_

- [ ] 20. 비동기 처리 구현
  - [ ] 20.1 Kafka 설정 및 이벤트 발행
    - 콘텐츠 등록/수정 이벤트
    - _Requirements: 21.1_
  - [ ] 20.2 검색 인덱스 동기화 리스너
    - Elasticsearch 인덱스 자동 업데이트
    - _Requirements: 19.7_
  - [ ] 20.3 알림 이벤트 리스너
    - 신고 자동 비공개 알림
    - _Requirements: 15.4_

- [ ] 21. API 문서화
  - [ ] 21.1 OpenAPI/Swagger 문서 생성
    - _Requirements: 21.1_

## Notes

- MVP 단계로 Property 테스트 및 Checkpoint 생략
- Each task references specific requirements for traceability
- 테스트는 추후 안정화 단계에서 추가 예정
