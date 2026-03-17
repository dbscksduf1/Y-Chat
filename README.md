# 💬 Y-Chat 1인개발

> WebSocket과 Redis Pub/Sub 기반의 실시간 채팅 및 랜덤채팅 서비스

---

## 프로젝트 소개

Y-Chat은 사용자 간 실시간 채팅과 랜덤채팅 기능을 제공하는 웹 서비스입니다.  
WebSocket 기반의 양방향 통신과 Redis Pub/Sub 구조를 적용하여  
다수 사용자 환경에서도 안정적인 메시지 전달이 가능하도록 설계했습니다.

---

## 기술 스택

- Backend: Spring Boot, WebSocket, Redis, JWT
- Frontend: React
- Database: PostgreSQL


---

## 주요 기능

- JWT 기반 회원 인증 / 로그인
- 1:1 채팅 및 채팅방 생성
- Queue 기반 랜덤 채팅 매칭
- 친구 추가 / 친구 목록 조회
- 사용자 차단 기능
- 프로필 이미지 업로드
- 온라인 상태 표시

---

## ⚙️ 시스템 아키텍처

<img width="794" height="650" alt="image" src="https://github.com/user-attachments/assets/8d9c801a-0125-4e3d-a681-f8847e77f488" />

```text
Client (React)
   ↓
WebSocket (STOMP)
   ↓
Spring Boot
   ↓
Redis Pub/Sub
   ↓
DB (PostgreSQL)

🔥 핵심 구현 및 성능 개선
1️⃣ WebSocket 기반 실시간 채팅

HTTP polling 방식의 비효율을 개선하기 위해 WebSocket을 도입

양방향 통신 구조를 통해 실시간 메시지 처리 구현

2️⃣ Redis Pub/Sub을 활용한 메시지 브로드캐스트

단일 서버 구조에서 발생하는 메시지 처리 병목 문제 해결

Redis Pub/Sub을 통해 서버 간 메시지 동기화 및 확장성 확보

👉 결과

다중 서버 환경에서도 안정적인 메시지 전달 가능

실시간 처리 성능 개선

3️⃣ 읽음 처리(UnreadCount) 최적화

메시지마다 발생하던 DB 업데이트 문제 해결

읽음 처리 로직을 개선하여 불필요한 DB write 감소

👉 결과

DB 부하 감소

채팅 트래픽 증가 시 안정성 확보

4️⃣ 클라이언트 상태 관리 개선 (sessionStorage)

localStorage 사용 시 발생한 다중 탭 계정 충돌 문제 해결

sessionStorage를 적용하여 사용자 세션 분리

👉 결과

계정 혼선 문제 해결

클라이언트 안정성 향상
