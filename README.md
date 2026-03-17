# 💬 Y-Chat 1인개발

> WebSocket과 Redis Pub/Sub 기반의 실시간 채팅 및 랜덤채팅 서비스

---
# 📑 목차

- [프로젝트 소개](#intro)
- [기술 스택](#stack)
- [주요 기능](#feature)
- [시스템 아키텍처](#arch)
- [핵심 구현 및 성능 리팩토링](#core)
- [서비스화면](#ui)
- [포트폴리오 문제 해결](#problem)
- [트러블슈팅](#trouble)
- [회고](#review)
  ---

# 프로젝트 소개 <a name="intro"></a>

Y-Chat은 사용자 간 실시간 채팅과 랜덤채팅 기능을 제공하는 웹 서비스입니다.  
WebSocket 기반의 양방향 통신과 Redis Pub/Sub 구조를 적용하여  
다수 사용자 환경에서도 안정적인 메시지 전달이 가능하도록 설계했습니다.

---

# 기술 스택 <a name="stack"></a>

- Backend: Spring Boot, WebSocket, Redis, JWT
- Frontend: React
- Database: PostgreSQL


---

# 주요 기능 <a name="feature"></a>

- JWT 기반 회원 인증 / 로그인
- 1:1 채팅 및 채팅방 생성
- Queue 기반 랜덤 채팅 매칭
- 친구 추가 / 친구 목록 조회
- 사용자 차단 기능
- 프로필 이미지 업로드
- 온라인 상태 표시

---

# 시스템 아키텍처 <a name="arch"></a>

<p align="center">
  <img src="https://github.com/user-attachments/assets/589123ff-441f-4dec-b025-7b08d21c39ac"/>
</p>


### 1. User → React Client
사용자가 채팅 메시지를 입력하면 React Client가 메시지를 처리합니다.



### 2. React Client → WebSocket (STOMP)
React는 서버와 WebSocket 프로토콜(STOMP)로 연결합니다.



### 3. WebSocket → Spring Boot
WebSocket 메시지는 Spring Boot 서버로 전달합니다



### 4. Spring Boot → Redis Pub/Sub
채팅 메시지는 바로 사용자에게 전송되지 않고  
브로커 역할을 하는 Redis Pub/Sub으로 전달됩니다.



### 5. Redis Pub/Sub → WebSocket Broadcast
Redis Subscriber가 메시지를 수신한 후  
채팅방 사용자들에게 WebSocket Broadcast로 전달합니다



### 6. 메시지 저장 (Database)
채팅 메시지는 동시에 데이터베이스(PostgreSQL)에 저장됩니다.


---



# 핵심 구현 및 성능 리팩토링 <a name="core"></a>

### 1. WebSocket 기반 실시간 채팅

- HTTP polling 방식의 비효율을 개선하기 위해 WebSocket 도입  
- 양방향 통신 구조를 통해 실시간 메시지 처리 구현  



### 2. Redis Pub/Sub을 활용한 메시지 브로드캐스트

- 단일 서버 구조에서 발생하는 메시지 처리 병목 문제 해결  
- Redis Pub/Sub을 통해 서버 간 메시지 동기화 및 확장성 확보  

 **결과**

- 다중 서버 환경에서도 안정적인 메시지 전달 가능  
- 실시간 처리 성능 개선  


### 3. 읽음 처리(UnreadCount) 최적화

- 메시지마다 발생하던 DB 업데이트 문제 해결  
- 읽음 처리 로직을 개선하여 불필요한 DB write 감소  

 **결과**

- DB 부하 감소  
- 채팅 트래픽 증가 시 안정성 확보  



### 4. 클라이언트 상태 관리 개선 (sessionStorage)

- localStorage 사용 시 발생한 계정 충돌 문제 해결  
- sessionStorage를 적용하여 사용자 세션 분리  

 **결과**

- 계정 혼선 문제 해결  
- 클라이언트 안정성 향상

---
# 서비스 화면 <a name="ui"></a>
## 1. 로그인 및 친구목록
<img width="431" height="958" alt="image" src="https://github.com/user-attachments/assets/0a7dd91f-1bef-4c07-a738-691e93be1c8b" /> <img width="437" height="965" alt="image" src="https://github.com/user-attachments/assets/a71da81a-8829-481d-8da0-c4c3172deb64" /> 
## 2. 프로필 수정 및 채팅방 목록
<img width="432" height="958" alt="image" src="https://github.com/user-attachments/assets/24aa3345-a132-4b36-805a-316ac4021791" /> <img width="437" height="963" alt="image" src="https://github.com/user-attachments/assets/c813fb58-3a89-4cc8-bfbc-17cfcf1a4661" /> 
## 3. 채팅방 및 랜덤채팅 화면
<img width="448" height="961" alt="image" src="https://github.com/user-attachments/assets/971ad626-e4ef-4ac5-b260-98075cc08e3c" /> <img width="437" height="961" alt="image" src="https://github.com/user-attachments/assets/9b36f369-7775-49f8-842e-a5dd7c086723" /> 
<img width="423" height="960" alt="image" src="https://github.com/user-attachments/assets/932d31c7-cffc-462b-b88e-6b6f8a9057fd" /> <img width="436" height="957" alt="image" src="https://github.com/user-attachments/assets/c98b0d16-696f-42b3-8d81-6af99c81f815" />

---
# 포트폴리오 문제 해결 <a name="problem"></a>
## WebSocket을 활용하여 실시간 메시지처리 해결 <img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/500746ee-727d-4e5b-b6e5-2320f5e22318" />
## Redis Pub/Sub구조를 활용 -> 다중서버에서 안정적<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/32497df8-640a-4dc3-be4c-e8f17bb107fa" /> 
## Queue 자료구조 기반 랜덤 채팅 매칭 로직을 구현 <img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/33aa177d-5b61-43f2-8d62-d5534d2ba1e3" />


---



# 트러블슈팅 <a name="trouble"></a>

### 1. WebSocket 메시지 중복 및 구조 문제

- **문제**: WebSocket 메시지가 특정 환경에서 중복 전송되거나, 서버 확장 시 메시지가 일부 사용자에게 전달되지 않는 문제 발생  
- **원인**: 단일 서버 기반으로 메시지를 직접 브로드캐스트하는 구조로 인해 서버 간 메시지 공유가 불가능  
- **해결**: Redis Pub/Sub 구조를 도입하여 메시지를 중앙 브로커를 통해 처리하도록 개선  
- **결과**: 메시지 중복 문제를 해결하고, 다중 서버 환경에서도 안정적인 메시지 전달 가능  

실시간 시스템에서는 메시지 흐름과 구조 설계의 중요성을 느꼈다.

---

### 2. 읽음 처리(UnreadCount)로 인한 DB 부하 문제

- **문제**: 메시지 수신 시마다 읽음 처리로 인해 DB 업데이트가 과도하게 발생  
- **원인**: 메시지 단위로 읽음 상태를 즉시 반영하는 구조  
- **해결**: 읽음 처리 로직을 개선하여 불필요한 DB write를 줄이는 방식으로 변경  
- **결과**: DB 부하 감소 및 채팅 트래픽 증가 상황에서도 안정성 확보  
 
트래픽 증가 상황에 대비하여 안정적인 서버 운영의 필요성을 느꼈다.

---

### 3. 다중 탭 로그인 시 계정 충돌 문제

- **문제**: localStorage 사용 시 여러 탭에서 동일 계정 정보가 공유되어 계정이 섞이는 문제 발생  
- **원인**: 브라우저의 localStorage는 모든 탭에서 공유되는 구조  
- **해결**: sessionStorage로 변경하여 탭별로 세션을 분리  
- **결과**: 계정 충돌 문제 해결 및 사용자별 독립적인 세션 유지  

클라이언트 상태 관리 방식도 서비스 안정성에 큰 영향을 준다는 것을 경험했다.

---

# 회고 <a name="review"></a>

단순한 채팅 기능 구현을 넘어, 실시간 시스템에서 발생하는 다양한 문제를 해결하며 구조와 성능을 함께 고민하고 해결했다.
특히 WebSocket과 Redis Pub/Sub 구조를 적용하면서 확장 가능한 시스템 설계의 중요성을 이해하게 되었고, 데이터 흐름과 병목 지점을 분석하는 경험을 했다.
