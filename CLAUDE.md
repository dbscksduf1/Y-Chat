# yunchat — CLAUDE.md

Spring Boot + React/TypeScript 기반 실시간 채팅 앱.

## 기술 스택

- **Backend**: Spring Boot, STOMP WebSocket, Redis Pub/Sub, JPA + QueryDSL, JWT
- **Frontend**: React + TypeScript, @stomp/stompjs
- **DB**: JPA (RDBMS), Redis (Pub/Sub, 랜덤채팅 큐)

## 패키지 구조

```
backend/src/main/java/com/yunchat/chat/
├── domain/
│   ├── chat/        # 채팅방, 메시지, 읽음 처리
│   ├── friend/      # 친구 요청/수락
│   ├── random/      # 랜덤 채팅 매칭
│   └── user/        # 회원가입, 로그인, 차단
└── global/
    ├── config/      # WebSocket, Security, QueryDSL 설정
    ├── exception/   # 전역 예외처리
    ├── jwt/         # JWT 필터, 프로바이더
    ├── redis/       # Pub/Sub 발행/구독
    └── websocket/   # STOMP 인터셉터, 온라인 유저 상태
```

## 주요 기능

- 일반 채팅방 (그룹, 1:1)
- 랜덤 채팅 (DB 저장 없음, 인메모리)
- 친구/차단
- 읽음 처리 (MessageRead 엔티티)
- 커서 기반 무한스크롤 페이지네이션

---

## 수정 이력

### Redis Pub/Sub 버그 수정
- **파일**: `RedisSubscriber.java`
- **문제**: `chatMessage.getId()`(메시지 ID)를 roomId로 사용 → 잘못된 topic으로 broadcast
- **수정**: `chatMessage.getRoomId()` 사용
- **관련**: `ChatMessageResponse`에 `roomId` 필드 추가, 모든 생성 지점에 roomId 전달

### ObjectMapper 싱글톤화
- **파일**: `RedisConfig.java`, `RedisSubscriber.java`, `RedisPublisher.java`
- **문제**: `RedisSubscriber`는 인스턴스 필드로 ObjectMapper를 직접 생성, `RedisPublisher`는 publish 호출마다 `registerModule` 재등록
- **수정**: `RedisConfig`에 `ObjectMapper` 빈 등록 (JavaTimeModule 포함), 두 클래스 모두 빈 주입으로 변경

### N+1 쿼리 제거 — getMyChatRooms
- **파일**: `ChatMessageService.java`, `RoomMemberRepository.java`, `ChatMessageRepository.java`
- **문제**: 채팅방 N개 × (멤버 조회 + 마지막 메시지 조회) = 2N+1 쿼리
- **수정**:
  - `RoomMemberRepository.findByRoomIds(List<Long>)` 추가
  - `ChatMessageRepository.findLastMessagesPerRoom(List<Long>)` 추가
  - 서비스에서 Map으로 변환 후 루프에서 사용 → **고정 3 쿼리**

### N+1 쿼리 제거 — markAsRead / markAsReadAndReturnMessages
- **파일**: `ChatMessageService.java`
- **문제**: 방의 전체 메시지 로드 후 각 메시지마다 `existsByMessageAndUsername` + `save` = O(N) 쿼리
- **수정**: 이미 존재하는 `findUnreadMessages` (NOT EXISTS 서브쿼리로 미읽음 필터링) + `saveAll` 배치 저장 → **2 쿼리**

### 매직넘버 제거
- **파일**: `ChatController.java`
- **문제**: `roomId > 1000000000000L` 조건으로 랜덤채팅 판별 — 서버 재시작 시 인메모리 상태 소실 시 오작동
- **수정**: `randomChatService.getRoom(roomId) != null` 조건만 사용

### 랜덤채팅 chat.enter 이벤트 버그
- **파일**: `ChatRoomPage.tsx`
- **문제**: 랜덤채팅 입장 시에도 `/app/chat.enter` 이벤트 전송 → 불필요한 DB 읽음 처리 시도
- **수정**: `isRandom`이 아닐 때만 전송

### JWT 토큰 URL 노출 수정
- **파일**: `JwtHandshakeInterceptor.java`, `WebSocketUserInterceptor.java`, `ChatRoomPage.tsx`
- **문제**: WebSocket URL에 `?token=...` 쿼리파라미터로 JWT 전달 → 서버 접근 로그에 토큰 평문 노출
- **수정**:
  - `JwtHandshakeInterceptor`: URL 토큰 검증 제거, 항상 handshake 허용
  - `WebSocketUserInterceptor`: STOMP CONNECT 프레임의 `Authorization: Bearer ...` 헤더에서 토큰 검증 및 Principal 설정
  - `StompAuthChannelInterceptor`: 중복 역할 → 삭제
  - 프론트엔드: URL 쿼리파라미터 제거, `connectHeaders: { Authorization: "Bearer ..." }` 사용

### 랜덤채팅 상태 Redis 마이그레이션
- **파일**: `RandomChatService.java`
- **문제**: `rooms`, `userRoom` Map이 JVM 인메모리 → 서버 재시작/멀티 인스턴스 배포 시 상태 소실
- **수정**:
  - `random:room:{roomId}` → Redis String (`userA|userB` 형식, TTL 24시간)
  - `random:user:{email}` → Redis String (roomId, TTL 24시간)
  - ConcurrentHashMap 완전 제거

### 하드코딩된 서버 URL 환경변수 분리
- **파일**: `frontend/.env`, `axios.ts`, `ChatRoomPage.tsx`
- **문제**: `https://y-chat-my45.onrender.com` URL이 여러 파일에 하드코딩
- **수정**: `VITE_API_BASE_URL`, `VITE_WS_URL` 환경변수로 분리

### 코드 정리
- `ChatMessageService.java`: `PageRequest` 중복 import 제거
- `OnlineUserService.java`: `System.out.println` → SLF4J (`log.debug`)
- `RedisSubscriber.java`: `System.out.println` → SLF4J
- `WebSocketEventListener.java`: `System.out.println` → SLF4J
- `ChatRoomPage.tsx`: `messages` 타입 `any[]` → `ChatMessage` 인터페이스, 미사용 `sorted` 변수 제거

---

## 남은 알려진 문제점

1. **unreadCount 부정확** (`ChatMessageService.saveMessage`)
   - 저장 시점 unreadCount = 멤버 수 - 1 (이미 읽은 사람 고려 안 함)
   - 현재는 실시간 읽음 처리 이벤트로 덮어써지므로 실제 영향 없음
