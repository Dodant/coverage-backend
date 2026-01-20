# 요구사항 문서

## 소개

SocialServiceImpl 클래스의 성능 및 트랜잭션 문제를 해결하기 위한 최적화 요구사항입니다. 현재 구현에서 발견된 N+1 쿼리 문제와 트랜잭션 경계 이슈를 개선하여 데이터베이스 성능을 향상시키고 데이터 일관성을 보장합니다.

## 용어집

- **Social_Service**: 좋아요 및 댓글 기능을 제공하는 서비스 컴포넌트
- **BookCover_Repository**: BookCover 엔티티에 대한 데이터 접근을 제공하는 Repository
- **Redesign_Repository**: Redesign 엔티티에 대한 데이터 접근을 제공하는 Repository
- **Atomic_Update_Query**: 단일 SQL UPDATE 문으로 카운트를 증감하는 쿼리
- **N+1_Query_Problem**: 하나의 작업에 대해 불필요하게 여러 번의 쿼리가 발생하는 성능 문제
- **Transactional_Boundary**: 트랜잭션의 시작과 끝을 정의하는 경계

## 요구사항

### 요구사항 1: 좋아요 카운트 원자적 증가

**사용자 스토리:** 개발자로서, 좋아요 추가 시 단일 UPDATE 쿼리로 카운트를 증가시키고 싶습니다. 이를 통해 불필요한 SELECT 쿼리를 제거하고 성능을 개선할 수 있습니다.

#### 인수 조건

1. WHEN 좋아요가 추가될 때, THE BookCover_Repository SHALL 단일 UPDATE 쿼리로 likeCount를 1 증가시킨다
2. WHEN 좋아요가 추가될 때, THE Redesign_Repository SHALL 단일 UPDATE 쿼리로 likeCount를 1 증가시킨다
3. WHEN 원자적 증가 쿼리가 실행될 때, THE Atomic_Update_Query SHALL 영향받은 행 수를 반환한다
4. IF 대상 엔티티가 존재하지 않으면, THEN THE Atomic_Update_Query SHALL 0을 반환한다

### 요구사항 2: 좋아요 카운트 원자적 감소

**사용자 스토리:** 개발자로서, 좋아요 취소 시 단일 UPDATE 쿼리로 카운트를 감소시키고 싶습니다. 이를 통해 불필요한 SELECT 쿼리를 제거하고 성능을 개선할 수 있습니다.

#### 인수 조건

1. WHEN 좋아요가 취소될 때, THE BookCover_Repository SHALL 단일 UPDATE 쿼리로 likeCount를 1 감소시킨다
2. WHEN 좋아요가 취소될 때, THE Redesign_Repository SHALL 단일 UPDATE 쿼리로 likeCount를 1 감소시킨다
3. WHILE likeCount가 0보다 클 때, THE Atomic_Update_Query SHALL likeCount를 감소시킨다
4. WHILE likeCount가 0일 때, THE Atomic_Update_Query SHALL likeCount를 0으로 유지한다

### 요구사항 3: 댓글 카운트 원자적 증가

**사용자 스토리:** 개발자로서, 댓글 추가 시 단일 UPDATE 쿼리로 카운트를 증가시키고 싶습니다. 이를 통해 불필요한 SELECT 쿼리를 제거하고 성능을 개선할 수 있습니다.

#### 인수 조건

1. WHEN 댓글이 추가될 때, THE BookCover_Repository SHALL 단일 UPDATE 쿼리로 commentCount를 1 증가시킨다
2. WHEN 댓글이 추가될 때, THE Redesign_Repository SHALL 단일 UPDATE 쿼리로 commentCount를 1 증가시킨다
3. WHEN 원자적 증가 쿼리가 실행될 때, THE Atomic_Update_Query SHALL 영향받은 행 수를 반환한다

### 요구사항 4: 댓글 카운트 원자적 감소

**사용자 스토리:** 개발자로서, 댓글 삭제 시 단일 UPDATE 쿼리로 카운트를 감소시키고 싶습니다. 이를 통해 불필요한 SELECT 쿼리를 제거하고 성능을 개선할 수 있습니다.

#### 인수 조건

1. WHEN 댓글이 삭제될 때, THE BookCover_Repository SHALL 단일 UPDATE 쿼리로 commentCount를 1 감소시킨다
2. WHEN 댓글이 삭제될 때, THE Redesign_Repository SHALL 단일 UPDATE 쿼리로 commentCount를 1 감소시킨다
3. WHILE commentCount가 0보다 클 때, THE Atomic_Update_Query SHALL commentCount를 감소시킨다
4. WHILE commentCount가 0일 때, THE Atomic_Update_Query SHALL commentCount를 0으로 유지한다

### 요구사항 5: SocialServiceImpl 리팩토링

**사용자 스토리:** 개발자로서, SocialServiceImpl이 새로운 원자적 쿼리 메서드를 사용하도록 리팩토링하고 싶습니다. 이를 통해 N+1 쿼리 문제를 해결하고 코드를 단순화할 수 있습니다.

#### 인수 조건

1. WHEN toggleLike 메서드가 호출될 때, THE Social_Service SHALL 원자적 증가/감소 쿼리를 사용한다
2. WHEN createComment 메서드가 호출될 때, THE Social_Service SHALL 원자적 증가 쿼리를 사용한다
3. WHEN deleteComment 메서드가 호출될 때, THE Social_Service SHALL 원자적 감소 쿼리를 사용한다
4. THE Social_Service SHALL find-then-save 패턴을 사용하지 않는다

### 요구사항 6: 트랜잭션 경계 보장

**사용자 스토리:** 개발자로서, 모든 쓰기 작업이 적절한 트랜잭션 경계 내에서 실행되도록 보장하고 싶습니다. 이를 통해 데이터 일관성을 유지하고 롤백이 올바르게 동작하도록 합니다.

#### 인수 조건

1. WHEN toggleLike 메서드가 실행될 때, THE Social_Service SHALL 좋아요 저장/삭제와 카운트 업데이트를 동일 트랜잭션 내에서 처리한다
2. WHEN createComment 메서드가 실행될 때, THE Social_Service SHALL 댓글 저장과 카운트 업데이트를 동일 트랜잭션 내에서 처리한다
3. WHEN deleteComment 메서드가 실행될 때, THE Social_Service SHALL 댓글 삭제와 카운트 업데이트를 동일 트랜잭션 내에서 처리한다
4. IF 트랜잭션 중 예외가 발생하면, THEN THE Social_Service SHALL 모든 변경사항을 롤백한다

### 요구사항 7: 존재하지 않는 대상 처리

**사용자 스토리:** 개발자로서, 존재하지 않는 대상에 대한 카운트 업데이트 시도를 명시적으로 처리하고 싶습니다. 이를 통해 디버깅을 용이하게 하고 시스템 동작을 예측 가능하게 합니다.

#### 인수 조건

1. WHEN 존재하지 않는 BookCover에 대해 카운트 업데이트가 시도될 때, THE Social_Service SHALL 작업을 조용히 무시한다
2. WHEN 존재하지 않는 Redesign에 대해 카운트 업데이트가 시도될 때, THE Social_Service SHALL 작업을 조용히 무시한다
3. THE Atomic_Update_Query SHALL 영향받은 행 수를 반환하여 업데이트 성공 여부를 확인할 수 있게 한다
