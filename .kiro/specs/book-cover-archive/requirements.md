# Requirements Document

## Introduction

북커버 디자인 아카이브는 Mobbin과 같은 UI/UX 디자인 레퍼런스 사이트를 북커버에 특화시킨 플랫폼입니다. 원서의 북커버와 각 국가 번역서의 북커버를 쉽게 비교할 수 있으며, 북커버 디자이너와 디자인 지망생들을 위한 영감과 레퍼런스를 제공합니다. 책 내용이 아닌 오로지 책 커버 디자인에만 초점을 맞춥니다.

## Glossary

- **Original_Book**: 원서 정보를 담는 엔티티로, 책의 기본 정보와 원서 커버 이미지를 포함
- **Translated_Book**: 번역서 정보를 담는 엔티티로, 원서와 연관되어 각국 번역판의 커버 정보를 포함
- **Redesign**: 회원이 기존 북커버를 재해석하여 업로드한 디자인 작품
- **User**: 시스템에 등록된 회원으로, 역할(출판사, 작가, 디자이너, 일반회원)을 가짐
- **Contributor**: 10권 이상의 책을 아카이브에 등록한 일반 회원에게 부여되는 칭호
- **Book_Cover**: 책의 앞면 또는 뒷면 커버 이미지
- **ISBN**: 국제표준도서번호 (International Standard Book Number), 책을 고유하게 식별하는 13자리 또는 10자리 코드
- **Genre**: 책의 장르 (예: 소설, 에세이, 시, 자기계발, SF, 판타지 등)
- **Edition**: 같은 책의 다른 출판 버전 (예: 초판, 개정판, 특별판)
- **Collection**: 회원이 생성하는 북커버 모음 폴더로, 개인 레퍼런스 저장 및 공유 목적으로 사용
- **Archive_System**: 북커버 데이터를 저장하고 관리하는 전체 시스템
- **Social_Feature**: 좋아요와 댓글 기능을 포함하는 사용자 상호작용 기능
- **Admin_Console**: 관리자 전용 운영 도구 및 대시보드

## Requirements

### Requirement 1: 원서 등록 및 관리

**User Story:** As a 회원, I want to 원서 정보와 북커버를 등록하고 관리할 수 있기를, so that 북커버 아카이브에 새로운 원서 데이터를 추가할 수 있다.

#### Acceptance Criteria

1.1. WHEN 회원이 원서 등록 폼을 제출하면 THEN THE Archive_System SHALL 책 제목, 원서 제목, 나라, 언어, 연도, 작가, 출판사, 장르, 앞면 북커버를 필수 항목으로 검증한다
1.2. WHERE ISBN이 제공되는 경우 THE Archive_System SHALL ISBN 형식을 검증하고 저장한다
1.3. WHEN 앞면 북커버 이미지가 누락된 경우 THEN THE Archive_System SHALL 등록을 거부하고 오류 메시지를 표시한다
1.4. WHERE 뒷면 북커버가 제공되는 경우 THE Archive_System SHALL 해당 이미지를 선택적으로 저장한다
1.5. WHEN 원서가 성공적으로 등록되면 THEN THE Archive_System SHALL 고유 식별자를 생성하고 데이터베이스에 저장한다
1.6. WHERE 북커버 디자이너 정보가 제공되면 THE Archive_System SHALL 디자이너 정보를 원서와 연결하여 저장한다
1.7. WHERE 에디션 정보가 제공되면 THE Archive_System SHALL 에디션 정보를 원서와 연결하여 저장한다

### Requirement 2: 번역서 등록 및 관리

**User Story:** As a 회원, I want to 원서와 연관된 번역서 정보를 등록할 수 있기를, so that 같은 책의 다양한 국가 버전 커버를 비교할 수 있다.

#### Acceptance Criteria

2.1. WHEN 회원이 번역서를 등록하면 THEN THE Archive_System SHALL 해당 번역서를 기존 원서와 연결한다
2.2. WHEN 번역서 등록 폼을 제출하면 THEN THE Archive_System SHALL 각국 책 제목, 나라, 언어, 연도, 작가, 옮긴이, 출판사, 앞면 북커버를 필수 항목으로 검증한다
2.3. WHERE ISBN이 제공되는 경우 THE Archive_System SHALL ISBN 형식을 검증하고 저장한다
2.4. WHERE 뒷면 북커버가 제공되는 경우 THE Archive_System SHALL 해당 이미지를 선택적으로 저장한다
2.5. WHERE 에디션 정보가 제공되면 THE Archive_System SHALL 에디션 정보를 번역서와 연결하여 저장한다
2.6. WHEN 원서에 여러 번역서가 등록되면 THEN THE Archive_System SHALL 모든 번역서를 원서 상세 페이지에서 조회 가능하게 한다

### Requirement 3: 리디자인 기능

**User Story:** As a 회원, I want to 기존 북커버를 재해석한 나만의 디자인을 업로드할 수 있기를, so that 다른 회원들과 아이디어를 공유하고 피드백을 받을 수 있다.

#### Acceptance Criteria

3.1. WHEN 회원이 리디자인을 업로드하면 THEN THE Archive_System SHALL 해당 리디자인을 원서 또는 번역서와 연결한다
3.2. WHEN 리디자인 등록 폼을 제출하면 THEN THE Archive_System SHALL 리디자인 이미지, 설명을 필수 항목으로 검증한다
3.3. WHEN 리디자인이 성공적으로 등록되면 THEN THE Archive_System SHALL 원본 커버와 함께 비교 가능하도록 표시한다
3.4. THE Archive_System SHALL 각 원서/번역서에 연결된 모든 리디자인 목록을 조회할 수 있게 한다
3.5. THE Archive_System SHALL 한 책에 대해 여러 회원이 각각 리디자인을 업로드하는 것을 허용한다
3.6. THE Archive_System SHALL 한 회원이 같은 책에 대해 여러 개의 리디자인을 업로드하는 것을 허용한다
3.7. WHEN 리디자인이 등록되면 THEN THE Archive_System SHALL 업로드한 회원 정보를 리디자인과 연결하여 저장한다

### Requirement 4: 소셜 기능 - 좋아요

**User Story:** As a 회원, I want to 마음에 드는 북커버 디자인에 좋아요를 누를 수 있기를, so that 선호하는 디자인을 표시하고 인기 있는 디자인을 확인할 수 있다.

#### Acceptance Criteria

4.1. WHEN 회원이 북커버에 좋아요를 누르면 THEN THE Social_Feature SHALL 해당 회원의 좋아요를 기록하고 좋아요 수를 증가시킨다
4.2. WHEN 회원이 이미 좋아요를 누른 북커버에 다시 좋아요를 누르면 THEN THE Social_Feature SHALL 좋아요를 취소하고 좋아요 수를 감소시킨다
4.3. THE Social_Feature SHALL 원서 커버, 번역서 커버, 리디자인 각각에 대해 독립적인 좋아요 기능을 제공한다
4.4. WHEN 북커버 목록을 조회하면 THEN THE Archive_System SHALL 각 커버의 좋아요 수를 함께 표시한다

### Requirement 5: 소셜 기능 - 댓글

**User Story:** As a 회원, I want to 북커버 디자인에 댓글을 달 수 있기를, so that 디자인에 대한 의견을 공유하고 토론할 수 있다.

#### Acceptance Criteria

5.1. WHEN 회원이 댓글을 작성하면 THEN THE Social_Feature SHALL 댓글 내용, 작성자, 작성 시간을 저장한다
5.2. WHEN 댓글이 빈 문자열이거나 공백만 포함하면 THEN THE Social_Feature SHALL 댓글 등록을 거부한다
5.3. THE Social_Feature SHALL 원서 커버, 번역서 커버, 리디자인 각각에 대해 독립적인 댓글 기능을 제공한다
5.4. WHEN 회원이 자신의 댓글을 삭제하면 THEN THE Social_Feature SHALL 해당 댓글을 데이터베이스에서 제거한다
5.5. WHEN 북커버 상세 페이지를 조회하면 THEN THE Archive_System SHALL 해당 커버의 모든 댓글을 시간순으로 표시한다

### Requirement 6: 다중 에디션 처리

**User Story:** As a 회원, I want to 같은 책의 여러 출판사 버전을 각각 등록할 수 있기를, so that 고전 문학처럼 다양한 출판사에서 출간된 책들의 커버를 비교할 수 있다.

#### Acceptance Criteria

6.1. WHEN 같은 원서에 대해 다른 출판사의 에디션을 등록하면 THEN THE Archive_System SHALL 각 에디션을 독립적인 엔트리로 저장하되 같은 작품으로 그룹화한다
6.2. THE Archive_System SHALL 원서만 있는 경우에도 아카이브에 등록을 허용한다
6.3. THE Archive_System SHALL 원서와 번역서를 함께 등록하는 것을 허용한다
6.4. WHEN 같은 작품의 여러 에디션을 조회하면 THEN THE Archive_System SHALL 모든 에디션을 그룹화하여 표시한다

### Requirement 7: 회원 시스템

**User Story:** As a 사용자, I want to 회원가입 시 역할을 선택하고 프로필 정보를 등록할 수 있기를, so that 내 역할에 맞는 기능과 권한을 사용하고 다른 회원들과 연결될 수 있다.

#### Acceptance Criteria

7.1. WHEN 사용자가 회원가입을 하면 THEN THE Archive_System SHALL 출판사, 작가, 디자이너, 일반회원 중 하나의 역할을 선택하도록 요구한다
7.2. WHERE 웹사이트 URL이 제공되는 경우 THE Archive_System SHALL URL 형식을 검증하고 저장한다
7.3. WHERE 포트폴리오 URL이 제공되는 경우 THE Archive_System SHALL URL 형식을 검증하고 저장한다
7.4. WHERE SNS 계정 정보가 제공되는 경우 THE Archive_System SHALL 해당 정보를 저장한다
7.5. WHEN 일반 회원이 10권 이상의 책을 아카이브에 등록하면 THEN THE Archive_System SHALL 해당 회원의 칭호를 "컨트리뷰터"로 자동 변경한다
7.6. THE Archive_System SHALL 회원의 등록 기여 횟수를 추적하고 저장한다
7.7. WHEN 회원 프로필을 조회하면 THEN THE Archive_System SHALL 역할, 칭호, 기여 횟수, 웹사이트, 포트폴리오, SNS 정보를 표시한다
7.8. THE Archive_System SHALL 회원이 프로필 정보를 수정할 수 있게 한다

### Requirement 8: 검색 및 필터링

**User Story:** As a 사용자, I want to 다양한 조건으로 북커버를 검색하고 필터링할 수 있기를, so that 원하는 북커버를 쉽게 찾을 수 있다.

#### Acceptance Criteria

8.1. WHEN 사용자가 검색어를 입력하면 THEN THE Archive_System SHALL 책 제목, 작가, 출판사, 디자이너 이름, ISBN으로 검색 결과를 반환한다
8.2. WHEN 사용자가 필터를 적용하면 THEN THE Archive_System SHALL 나라, 언어, 연도, 출판사, 장르별로 결과를 필터링한다
8.3. THE Archive_System SHALL 검색 결과를 좋아요 수, 등록일, 연도순으로 정렬하는 옵션을 제공한다
8.4. WHEN 검색 결과가 없으면 THEN THE Archive_System SHALL 적절한 안내 메시지를 표시한다
8.5. THE Archive_System SHALL 국가별 브라우징 페이지를 제공하여 특정 국가의 모든 북커버를 조회할 수 있게 한다
8.6. THE Archive_System SHALL 연도별 브라우징 페이지를 제공하여 특정 연도의 모든 북커버를 조회할 수 있게 한다
8.7. THE Archive_System SHALL 장르별 브라우징 페이지를 제공하여 특정 장르의 모든 북커버를 조회할 수 있게 한다
8.8. WHEN 국가별/연도별/장르별 페이지를 조회하면 THEN THE Archive_System SHALL 해당 카테고리의 북커버 수와 목록을 표시한다

### Requirement 9: 북커버 비교 기능

**User Story:** As a 디자이너, I want to 원서와 번역서의 북커버를 나란히 비교할 수 있기를, so that 각 국가별 디자인 차이를 분석할 수 있다.

#### Acceptance Criteria

9.1. WHEN 원서 상세 페이지를 조회하면 THEN THE Archive_System SHALL 원서 커버와 모든 번역서 커버를 나란히 표시한다
9.2. THE Archive_System SHALL 사용자가 비교할 커버를 선택하여 확대 비교할 수 있는 기능을 제공한다
9.3. WHEN 리디자인이 있는 경우 THEN THE Archive_System SHALL 원본 커버와 리디자인을 함께 비교할 수 있게 한다

### Requirement 10: 엔티티별 모아보기 기능

**User Story:** As a 사용자, I want to 특정 출판사, 작가, 디자이너의 작품들을 모아볼 수 있기를, so that 관심 있는 대상의 모든 북커버를 한눈에 확인할 수 있다.

#### Acceptance Criteria

10.1. WHEN 사용자가 출판사 이름을 클릭하면 THEN THE Archive_System SHALL 해당 출판사에서 출간한 모든 책의 북커버 목록을 표시한다
10.2. WHEN 사용자가 작가 이름을 클릭하면 THEN THE Archive_System SHALL 해당 작가가 쓴 모든 책의 북커버 목록을 표시한다
10.3. WHEN 사용자가 디자이너 이름을 클릭하면 THEN THE Archive_System SHALL 해당 디자이너가 디자인한 모든 북커버 목록을 표시한다
10.4. THE Archive_System SHALL 출판사, 작가, 디자이너 각각의 전용 프로필 페이지를 제공한다
10.5. WHEN 엔티티 프로필 페이지를 조회하면 THEN THE Archive_System SHALL 해당 엔티티의 기본 정보와 관련 북커버 수를 표시한다


### Requirement 11: 컬렉션 기능

**User Story:** As a 회원, I want to 마음에 드는 북커버를 컬렉션에 저장하고 관리할 수 있기를, so that 나만의 레퍼런스 폴더를 만들고 다른 사람들과 공유할 수 있다.

#### Acceptance Criteria

11.1. WHEN 회원이 새 컬렉션을 생성하면 THEN THE Archive_System SHALL 컬렉션 이름, 설명, 공개/비공개 설정을 저장한다
11.2. WHEN 회원이 북커버를 컬렉션에 추가하면 THEN THE Archive_System SHALL 해당 북커버를 컬렉션과 연결한다
11.3. THE Archive_System SHALL 원서 커버, 번역서 커버, 리디자인 모두 컬렉션에 추가할 수 있게 한다
11.4. WHEN 회원이 컬렉션을 공개로 설정하면 THEN THE Archive_System SHALL 다른 사용자들이 해당 컬렉션을 조회할 수 있게 한다
11.5. WHEN 회원이 컬렉션을 비공개로 설정하면 THEN THE Archive_System SHALL 해당 회원만 컬렉션을 조회할 수 있게 한다
11.6. THE Archive_System SHALL 회원이 여러 개의 컬렉션을 생성하는 것을 허용한다
11.7. WHEN 회원이 컬렉션에서 북커버를 제거하면 THEN THE Archive_System SHALL 해당 연결을 삭제한다
11.8. WHEN 공개 컬렉션을 조회하면 THEN THE Archive_System SHALL 컬렉션 이름, 설명, 생성자, 포함된 북커버 목록을 표시한다
11.9. THE Archive_System SHALL 공개 컬렉션에 대해 공유 링크를 제공한다
11.10. WHEN 사용자가 컬렉션을 "링크 공유(읽기 전용)"로 설정하면 THEN THE Archive_System SHALL 링크 보유자가 조회할 수 있게 한다
11.11. THE Archive_System SHALL 다른 회원의 공개 컬렉션을 내 컬렉션으로 복제하는 기능을 제공한다

### Requirement 12: 중복 등록 처리

**User Story:** As a 회원, I want to 이미 등록된 책과 중복되는 책을 등록하려 할 때 적절한 안내를 받기를, so that 데이터 중복을 방지하고 기존 데이터와 연결할 수 있다.

#### Acceptance Criteria

12.1. WHEN 회원이 책을 등록하려 할 때 동일한 ISBN이 이미 존재하면 THEN THE Archive_System SHALL 중복 경고를 표시하고 기존 엔트리 정보를 보여준다
12.2. WHEN 회원이 책을 등록하려 할 때 제목, 저자, 연도가 유사한 책이 존재하면 THEN THE Archive_System SHALL 유사 항목 목록을 표시한다
12.3. WHEN 중복이 감지되면 THEN THE Archive_System SHALL "기존 엔트리에 새 에디션으로 추가", "새 엔트리로 등록", "등록 취소" 옵션을 제공한다
12.4. WHEN 회원이 "기존 엔트리에 새 에디션으로 추가"를 선택하면 THEN THE Archive_System SHALL 해당 책을 기존 작품의 새 에디션으로 연결한다
12.5. WHEN 회원이 "새 엔트리로 등록"을 선택하면 THEN THE Archive_System SHALL 독립적인 새 엔트리로 저장한다

### Requirement 13: 데이터 정규화 및 표준화

**User Story:** As a 시스템 관리자, I want to 데이터가 일관된 형식으로 저장되기를, so that 검색과 필터링이 정확하게 작동하고 데이터 품질이 유지된다.

#### Acceptance Criteria

13.1. WHEN 나라 정보를 입력하면 THEN THE Archive_System SHALL ISO 3166-1 국가 코드 형식으로 저장한다
13.2. WHEN 언어 정보를 입력하면 THEN THE Archive_System SHALL ISO 639-1 언어 코드 형식으로 저장한다
13.3. WHEN 연도를 입력하면 THEN THE Archive_System SHALL 4자리 연도 형식(YYYY)으로 검증하고 저장한다
13.4. THE Archive_System SHALL 출판사, 작가, 디자이너 이름에 대해 표준화된 표기법을 적용한다
13.5. WHEN 동일한 출판사/작가/디자이너가 다른 표기로 입력되면 THEN THE Archive_System SHALL 기존 엔티티와 연결할 수 있는 제안을 표시한다
13.6. THE Archive_System SHALL 나라와 언어 선택을 위한 표준화된 드롭다운 목록을 제공한다

### Requirement 14: 이미지 품질 규칙

**User Story:** As a 시스템 관리자, I want to 업로드되는 북커버 이미지가 일정 품질 기준을 충족하기를, so that 아카이브의 시각적 품질이 유지되고 사용자들이 고품질 레퍼런스를 얻을 수 있다.

#### Acceptance Criteria

14.1. WHEN 북커버 이미지를 업로드하면 THEN THE Archive_System SHALL 최소 해상도(800x1200 픽셀 이상)를 검증한다
14.2. WHEN 이미지 해상도가 최소 기준 미만이면 THEN THE Archive_System SHALL 업로드를 거부하고 권장 해상도를 안내한다
14.3. THE Archive_System SHALL 일반적인 북커버 비율(2:3, 1:1.5 등)을 권장하되 다른 비율도 허용한다
14.4. WHEN 이미지를 업로드하면 THEN THE Archive_System SHALL 지원되는 파일 형식(JPEG, PNG, WebP)을 검증한다
14.5. THE Archive_System SHALL 최대 파일 크기(10MB)를 제한한다
14.6. WHEN 이미지 파일 크기가 최대 제한을 초과하면 THEN THE Archive_System SHALL 업로드를 거부하고 파일 크기 제한을 안내한다
14.7. THE Archive_System SHALL 업로드된 이미지를 표준 색공간(sRGB)으로 변환하여 저장한다
14.8. THE Archive_System SHALL 썸네일과 원본 이미지를 별도로 저장하여 로딩 성능을 최적화한다

### Requirement 15: 콘텐츠 검수 및 신고 시스템

**User Story:** As a 시스템 관리자, I want to 부적절한 콘텐츠를 관리할 수 있기를, so that 아카이브의 품질과 신뢰성을 유지할 수 있다.

#### Acceptance Criteria

15.1. WHEN 회원이 새 콘텐츠를 등록하면 THEN THE Archive_System SHALL 즉시 공개 상태로 설정한다
15.2. WHEN 회원이 부적절한 콘텐츠를 발견하면 THEN THE Archive_System SHALL 신고 기능을 제공한다
15.3. WHEN 콘텐츠가 신고되면 THEN THE Archive_System SHALL 신고 사유, 신고자, 신고 시간을 기록한다
15.4. WHEN 동일 콘텐츠에 대해 3회 이상 신고가 접수되면 THEN THE Archive_System SHALL 해당 콘텐츠를 자동으로 비공개 처리하고 관리자에게 알린다
15.5. THE Admin_Console SHALL 관리자가 신고된 콘텐츠를 검토하고 "유지", "삭제", "수정 요청" 결정을 내릴 수 있게 한다
15.6. WHEN 관리자가 콘텐츠 삭제를 결정하면 THEN THE Archive_System SHALL 해당 콘텐츠를 삭제하고 등록자에게 사유를 통지한다
15.7. WHEN 관리자가 수정 요청을 결정하면 THEN THE Archive_System SHALL 등록자에게 수정 요청 사유를 통지한다
15.8. THE Archive_System SHALL 허위 신고를 반복하는 회원에 대해 신고 기능을 제한할 수 있게 한다

### Requirement 16: 수정 이력 및 감사 로그

**User Story:** As a 시스템 관리자, I want to 모든 데이터 변경 이력을 추적할 수 있기를, so that 운영 투명성을 확보하고 문제 발생 시 원인을 파악할 수 있다.

#### Acceptance Criteria

16.1. WHEN 회원 또는 관리자가 원서/번역서/리디자인 정보를 수정하면 THEN THE Archive_System SHALL 변경 전/후 값, 수정자, 수정 시간을 저장한다
16.2. THE Archive_System SHALL 엔티티 상세 페이지에서 "수정 이력"을 조회할 수 있게 한다
16.3. WHEN 관리자가 엔티티를 병합하면 THEN THE Archive_System SHALL 병합 작업 기록을 남긴다
16.4. WHEN 관리자가 엔티티를 분리하면 THEN THE Archive_System SHALL 분리 작업 기록을 남긴다
16.5. THE Archive_System SHALL 수정 이력 조회 권한을 역할에 따라 제한한다
16.6. THE Archive_System SHALL 감사 로그를 최소 1년간 보관한다

### Requirement 17: 저작권 및 삭제 요청 처리

**User Story:** As a 권리자, I want to 내 저작물에 대한 삭제 요청을 제출할 수 있기를, so that 저작권을 보호받을 수 있다.

#### Acceptance Criteria

17.1. THE Archive_System SHALL 권리자(출판사/대리인)의 삭제 요청 접수 폼을 제공한다
17.2. WHEN 삭제 요청이 접수되면 THEN THE Archive_System SHALL 요청자 정보, 대상 URL, 사유, 증빙을 기록한다
17.3. WHEN 관리자가 삭제 요청을 승인하면 THEN THE Archive_System SHALL 해당 이미지를 비공개 처리하고 재업로드 방지 플래그를 설정한다
17.4. WHEN 관리자가 삭제 요청을 거부하면 THEN THE Archive_System SHALL 거부 사유를 요청자에게 통지한다
17.5. WHERE 이미지 출처 정보가 제공되는 경우 THE Archive_System SHALL 출처(링크/촬영/스캔 여부)를 저장한다
17.6. THE Archive_System SHALL 삭제 요청 처리 이력을 보관한다

### Requirement 18: 어뷰징 및 스팸 방지

**User Story:** As a 시스템 관리자, I want to 악의적인 사용자 활동을 방지할 수 있기를, so that 시스템의 안정성과 데이터 품질을 유지할 수 있다.

#### Acceptance Criteria

18.1. THE Archive_System SHALL 댓글, 좋아요, 신고, 업로드에 대해 사용자 단위 레이트 리밋을 적용한다
18.2. WHEN 동일 사용자가 짧은 시간 내 반복 신고를 하면 THEN THE Archive_System SHALL 신고를 제한하고 캡차를 요구한다
18.3. WHEN 동일 사용자가 짧은 시간 내 대량 업로드를 시도하면 THEN THE Archive_System SHALL 업로드를 제한한다
18.4. THE Archive_System SHALL 의심 활동(봇 패턴)을 탐지하여 관리자에게 알린다
18.5. WHEN 레이트 리밋에 도달하면 THEN THE Archive_System SHALL 사용자에게 대기 시간을 안내한다
18.6. THE Admin_Console SHALL 반복적인 어뷰징 행위를 하는 계정을 일시 정지할 수 있게 한다

### Requirement 19: 관리자 콘솔 및 운영 도구

**User Story:** As a 시스템 관리자, I want to 전용 관리 도구를 사용할 수 있기를, so that 신고, 중복, 삭제 요청 등을 효율적으로 처리할 수 있다.

#### Acceptance Criteria

19.1. THE Admin_Console SHALL 관리자 전용 대시보드를 제공한다
19.2. THE Admin_Console SHALL 대시보드에서 신고 큐를 조회하고 처리할 수 있게 한다
19.3. THE Admin_Console SHALL 대시보드에서 중복 의심 항목 목록을 조회할 수 있게 한다
19.4. THE Admin_Console SHALL 대시보드에서 삭제 요청 목록을 조회하고 처리할 수 있게 한다
19.5. THE Admin_Console SHALL 엔티티 병합 기능을 제공한다
19.6. THE Admin_Console SHALL 출판사/작가/디자이너에 대한 별칭(alias) 관리 기능을 제공한다
19.7. THE Admin_Console SHALL 검색 인덱싱 상태를 조회할 수 있게 한다
19.8. THE Admin_Console SHALL 시스템 오류 로그를 조회할 수 있게 한다
19.9. THE Admin_Console SHALL 사용자 통계(가입, 활동, 기여)를 조회할 수 있게 한다

### Requirement 20: 디자인 속성 태깅 및 필터

**User Story:** As a 디자이너, I want to 디자인 속성으로 커버를 탐색할 수 있기를, so that 레퍼런스를 빠르게 찾을 수 있다.

#### Acceptance Criteria

20.1. THE Archive_System SHALL 커버에 대해 디자인 속성 태그(타이포/일러스트/사진/미니멀/레트로 등)를 부여할 수 있게 한다
20.2. THE Archive_System SHALL 색상(대표 색상), 레이아웃(중앙/좌정렬 등)과 같은 속성을 저장할 수 있다
20.3. WHEN 사용자가 태그/속성 필터를 적용하면 THEN THE Archive_System SHALL 결과를 필터링한다
20.4. THE Admin_Console SHALL 태그 사전 및 승인 정책을 관리할 수 있다
20.5. THE Archive_System SHALL 회원이 커버에 태그를 제안할 수 있게 한다
20.6. THE Archive_System SHALL 태그별 브라우징 페이지를 제공한다

### Requirement 21: 확장성 고려

**User Story:** As a 시스템 관리자, I want to 시스템이 향후 영화 포스터 등 다른 미디어로 확장 가능하도록 설계되기를, so that 최소한의 변경으로 새로운 콘텐츠 유형을 추가할 수 있다.

#### Acceptance Criteria

21.1. THE Archive_System SHALL 콘텐츠 유형을 추상화하여 북커버 외에 다른 미디어 유형을 추가할 수 있는 구조로 설계한다
21.2. THE Archive_System SHALL 공통 속성(제목, 이미지, 좋아요, 댓글)을 재사용 가능한 컴포넌트로 분리한다
21.3. THE Archive_System SHALL 미디어 유형별 고유 속성을 확장 가능한 방식으로 저장한다
