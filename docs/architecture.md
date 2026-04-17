# 🏗️ Architecture

## ChatterAI — 실시간 채팅 + AI 어시스턴트 플랫폼

> 이 문서는 시스템 구조, 인증 흐름, 데이터 흐름, 패키지 구조를 정의한다.  
> 구현 시 이 문서의 구조를 기준으로 패키지·컴포넌트를 생성한다.

---

## 목차

1. [시스템 전체 구조](#1-시스템-전체-구조)
2. [기술 레이어 구조](#2-기술-레이어-구조)
3. [인증 흐름](#3-인증-흐름)
4. [WebSocket 메시지 흐름](#4-websocket-메시지-흐름)
5. [@AI 멘션 처리 흐름](#5-ai-멘션-처리-흐름)
6. [Redis 아키텍처](#6-redis-아키텍처)
7. [백엔드 패키지 구조](#7-백엔드-패키지-구조)
8. [프론트엔드 컴포넌트 구조](#8-프론트엔드-컴포넌트-구조)
9. [배포 구성](#9-배포-구성)

---

## 1. 시스템 전체 구조

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client (Browser)                        │
│                      Vue.js 3 + Pinia                           │
│              HTTP(S) REST API  │  WSS (STOMP over WebSocket)    │
└────────────────────────────────┼────────────────────────────────┘
                                 │ HTTPS / WSS (443)
                    ┌────────────▼────────────┐
                    │      Spring Boot 3       │
                    │   Spring Security (JWT)  │
                    │   WebSocket (STOMP)      │
                    │   REST API Controllers   │
                    │   AI Service (MCP)       │
                    └──┬──────────┬───────────┘
                       │          │
            ┌──────────▼──┐  ┌───▼──────────┐
            │   MySQL 8   │  │   Redis 7    │
            │  (영속 데이터)│  │(Pub/Sub·캐시)│
            └─────────────┘  └─────────────┘
                                    │ Pub/Sub 브로드캐스트
                    ┌───────────────▼──────────────┐
                    │      Anthropic Claude API     │
                    │   (MCP via claude-sonnet)     │
                    └──────────────────────────────┘
```

---

## 2. 기술 레이어 구조

### 2.1 백엔드 레이어

```
┌──────────────────────────────────────────┐
│             Controller Layer              │  HTTP 요청/응답, WebSocket 메시지 수신
│  @RestController  @MessageMapping         │  비즈니스 로직 없음, DTO 변환만 담당
├──────────────────────────────────────────┤
│              Service Layer                │  비즈니스 로직, 권한 검증, 트랜잭션
│  @Service  @Transactional                 │  Redis 연동, AI 호출 오케스트레이션
├──────────────────────────────────────────┤
│            Repository Layer               │  DB 쿼리 (Spring Data JPA / JPQL)
│  JpaRepository  @Query                   │
├──────────────────────────────────────────┤
│              Entity Layer                 │  DB 매핑 전용, 비즈니스 로직 없음
│  @Entity  @Table                         │
├──────────────────────────────────────────┤
│               DTO Layer                   │  계층 간 데이터 전달, Entity 직접 노출 금지
│  RequestDto  ResponseDto                 │
└──────────────────────────────────────────┘
```

### 2.2 프론트엔드 레이어

```
┌──────────────────────────────────────────┐
│               Views (Pages)               │  라우터가 연결하는 최상위 페이지 컴포넌트
├──────────────────────────────────────────┤
│              Components                   │  재사용 UI 컴포넌트
│  layout/  chat/  common/  admin/          │
├──────────────────────────────────────────┤
│             Pinia Stores                  │  전역 상태 관리
│  auth  socket  channel  dm  ui           │
├──────────────────────────────────────────┤
│              API Modules                  │  Axios 기반 HTTP 요청 모음
│  auth  channel  message  dm  file  user  │
├──────────────────────────────────────────┤
│            Socket Service                 │  STOMP 클라이언트 관리
│  connect  subscribe  publish  reconnect  │
└──────────────────────────────────────────┘
```

---

## 3. 인증 흐름

### 3.1 로그인 및 Token 발급

```
Client                    Spring Boot               Redis / DB
  │                           │                         │
  │── POST /api/auth/login ──>│                         │
  │   { username, password }  │                         │
  │                           │── 비밀번호 검증 ────────>│ (DB: users)
  │                           │<── User 조회 ───────────│
  │                           │                         │
  │                           │── Access Token 생성 (30분, JWT)
  │                           │── Refresh Token 생성 (7일, UUID)
  │                           │── SET refresh:{userId} ─>│ (Redis, TTL 7일)
  │                           │                         │
  │<── 200 OK ────────────────│
  │    body: { accessToken, user }
  │    Set-Cookie: refresh={token}; HttpOnly; Secure; SameSite=Strict
  │                           │
  │ [메모리에 accessToken 저장]│
  │ [Cookie는 브라우저 자동 저장]
```

### 3.2 Access Token 갱신

```
Client                    Spring Boot               Redis
  │                           │                       │
  │── POST /api/auth/refresh ─>│                      │
  │   Cookie: refresh={token}  │                      │
  │                           │── GET refresh:{userId}─>│
  │                           │<── token 값 ───────────│
  │                           │   (일치 검증)           │
  │                           │── 새 Access Token 생성  │
  │                           │── SET refresh:{userId} ─>│ (기존 덮어쓰기, TTL 7일 갱신)
  │<── 200 OK ────────────────│                        │
  │    body: { accessToken }  │                        │
```

### 3.3 WebSocket 인증

```
Client                    Spring Boot (ChannelInterceptor)
  │                           │
  │── STOMP CONNECT ──────────>│
  │   headers:                 │
  │   Authorization: Bearer {accessToken}
  │                           │── JWT 파싱 및 검증
  │                           │── SecurityContext 설정
  │                           │
  │                           ├─ 유효 → CONNECTED 프레임 반환
  │<── CONNECTED ─────────────│
  │                           │
  │                           └─ 만료/무효 → ERROR 프레임 반환
  │<── ERROR ─────────────────│
  │   [Token 갱신 후 재연결]   │
```

---

## 4. WebSocket 메시지 흐름

### 4.1 채널 메시지 전송 전체 흐름

```
Client A              Spring Boot              Redis              Client B, C
   │                      │                     │                     │
   │─ SEND ───────────────>│                    │                     │
   │  /app/channel/1/send  │                    │                     │
   │  { content, type }    │                    │                     │
   │                       │── 메시지 DB 저장    │                     │
   │                       │── PUBLISH ─────────>│                    │
   │                       │   chat:channel:1    │                     │
   │                       │                     │── SUBSCRIBE 구독자 ─>│
   │<── /topic/channel/1 ──│<────────────────────│                     │
   │   (본인도 수신)         │                     │── /topic/channel/1 ─>│
   │                       │                     │                     │
   │                       │── 안읽은 수 INCR ───>│ (B, C의 unread 카운트)
```

### 4.2 STOMP 메시지 브로커 구조

```
┌──────────────────────────────────────────────────────────┐
│                   Spring Boot (STOMP)                     │
│                                                          │
│  Client SEND                  Server SEND                │
│  /app/**  ──────────────────> @MessageMapping            │
│                               Service 처리               │
│                               SimpMessagingTemplate      │
│                                    │                     │
│                          ┌─────────▼──────────┐          │
│                          │   Redis Pub/Sub     │          │
│                          │  (메시지 브로드캐스트)│          │
│                          └─────────┬──────────┘          │
│                                    │                     │
│  /topic/**  <──────────────────────┘                     │
│  /user/queue/**  <─── 개인 알림                           │
└──────────────────────────────────────────────────────────┘
```

---

## 5. @AI 멘션 처리 흐름

```
Client               Spring Boot           Redis          Anthropic API
  │                      │                   │                 │
  │─ SEND ───────────────>│                  │                 │
  │  "@AI 질문내용"        │                  │                 │
  │                       │── 메시지 DB 저장  │                 │
  │                       │── PUBLISH (사용자 메시지 브로드캐스트)
  │<── /topic/channel/1 ──│                  │                 │
  │   (사용자 메시지 표시) │                  │                 │
  │                       │                  │                 │
  │                       │── AI 로딩 메시지 브로드캐스트        │
  │<── /topic/channel/1 ──│                  │                 │
  │   (로딩 메시지 표시)   │                  │                 │
  │                       │                  │                 │
  │                       │── GET ai:usage:{userId} ──>│       │
  │                       │<── 사용횟수 확인 ──│               │
  │                       │                  │                 │
  │                       │── 채널 최근 10개 메시지 조회 (DB)   │
  │                       │                  │                 │
  │                       │── POST /v1/messages ──────────────>│
  │                       │   model: claude-sonnet-4-5         │
  │                       │   messages: [context + 질문]       │
  │                       │                  │                 │
  │                       │<── AI 응답 ───────────────────────│
  │                       │                  │                 │
  │                       │── AI 응답 DB 저장 │                 │
  │                       │── INCR ai:usage:{userId}:{date} ──>│
  │                       │── 로딩 메시지 교체 브로드캐스트      │
  │<── /topic/channel/1 ──│                  │                 │
  │   (AI 응답 표시)       │                  │                 │
```

---

## 6. Redis 아키텍처

### 6.1 용도별 데이터 구조

```
┌─────────────────────────────────────────────────────────────┐
│                        Redis 7                              │
│                                                             │
│  [Pub/Sub]                                                  │
│  chat:channel:{channelId}     채널 메시지 브로드캐스트 토픽  │
│  chat:dm:{dmRoomId}           DM 메시지 브로드캐스트 토픽   │
│                                                             │
│  [String - 온라인 상태]                                     │
│  presence:{userId}            "ONLINE" | "AWAY"  TTL: 60s  │
│                                                             │
│  [String - 안읽은 메시지 수]                                │
│  unread:{userId}:{channelId}  "3"                TTL: 없음  │
│                                                             │
│  [String - Refresh Token]                                   │
│  refresh:{userId}             UUID 문자열        TTL: 7일   │
│                                                             │
│  [String - AI 사용량]                                       │
│  ai:usage:{userId}:{date}     "15"               TTL: 24h  │
└─────────────────────────────────────────────────────────────┘
```

### 6.2 Redis Pub/Sub 메시지 브로드캐스트 원리

```
[Spring Boot 인스턴스 1]              [Spring Boot 인스턴스 2]
       │                                       │
  STOMP 수신                             STOMP 수신
  Client A 연결                         Client B 연결
       │                                       │
       │── PUBLISH chat:channel:1 ─> [Redis] ──│ SUBSCRIBE 수신
       │                                       │── /topic/channel/1 브로드캐스트
       │                                       │   Client B 수신
       │<── SUBSCRIBE 수신 ─────────────────── │
       │── /topic/channel/1 브로드캐스트
       │   Client A 수신
```

> MVP는 단일 인스턴스로 시작하나, Redis Pub/Sub 구조를 처음부터 적용해  
> 향후 수평 확장 시 코드 변경 없이 다중 인스턴스 운영이 가능하도록 설계한다.

---

## 7. 백엔드 패키지 구조

```
backend/src/main/java/com/chatterai/
│
├── config/
│   ├── SecurityConfig.java           # Spring Security, JWT 필터 설정
│   ├── WebSocketConfig.java          # STOMP 엔드포인트, 메시지 브로커 설정
│   ├── RedisConfig.java              # RedisTemplate, Pub/Sub 리스너 설정
│   ├── CorsConfig.java               # CORS 허용 설정
│   └── ClaudeApiConfig.java          # Anthropic API 클라이언트 설정
│
├── auth/
│   ├── controller/
│   │   └── AuthController.java       # /api/auth/**
│   ├── service/
│   │   └── AuthService.java
│   ├── dto/
│   │   ├── LoginRequestDto.java
│   │   ├── LoginResponseDto.java
│   │   └── JoinRequestDto.java
│   └── jwt/
│       ├── JwtProvider.java          # 토큰 생성·검증
│       ├── JwtAuthenticationFilter.java  # HTTP 요청 JWT 필터
│       └── StompAuthChannelInterceptor.java  # WebSocket STOMP JWT 검증
│
├── user/
│   ├── controller/
│   │   └── UserController.java       # /api/users/**
│   ├── service/
│   │   └── UserService.java
│   ├── repository/
│   │   └── UserRepository.java
│   ├── entity/
│   │   └── User.java                 # id, username, nickname, password, role, isActive
│   ├── dto/
│   │   └── UserResponseDto.java
│   └── enums/
│       └── Role.java                 # USER, ADMIN
│
├── channel/
│   ├── controller/
│   │   └── ChannelController.java    # /api/channels/**
│   ├── service/
│   │   └── ChannelService.java
│   ├── repository/
│   │   └── ChannelRepository.java
│   ├── entity/
│   │   └── Channel.java              # id, name, description, isPrivate, noticeMsgId
│   └── dto/
│       ├── ChannelResponseDto.java
│       └── ChannelCreateRequestDto.java
│
├── message/
│   ├── controller/
│   │   ├── MessageController.java    # /api/messages/** (REST: 수정·삭제·이모지)
│   │   └── ChatMessageController.java  # @MessageMapping (WebSocket 수신)
│   ├── service/
│   │   ├── MessageService.java
│   │   └── AiMessageService.java     # @AI 멘션 처리, Claude API 호출
│   ├── repository/
│   │   └── MessageRepository.java
│   ├── entity/
│   │   └── Message.java              # id, channelId, dmRoomId, userId, content, type, isDeleted
│   ├── dto/
│   │   ├── ChatMessageRequestDto.java   # STOMP 수신용
│   │   ├── ChatMessageResponseDto.java  # STOMP 발행용
│   │   └── MessageUpdateRequestDto.java
│   └── enums/
│       └── MessageType.java          # TEXT, FILE, AI_LOADING, AI_RESPONSE, AI_ERROR
│
├── dm/
│   ├── controller/
│   │   ├── DmController.java         # /api/dm/**
│   │   └── DmMessageController.java  # @MessageMapping /app/dm/**
│   ├── service/
│   │   └── DmService.java
│   ├── repository/
│   │   ├── DmRoomRepository.java
│   │   └── DmParticipantRepository.java
│   └── entity/
│       ├── DmRoom.java
│       └── DmParticipant.java
│
├── reaction/
│   ├── controller/
│   │   └── ReactionController.java   # /api/messages/{id}/reactions
│   ├── service/
│   │   └── ReactionService.java
│   ├── repository/
│   │   └── ReactionRepository.java
│   └── entity/
│       └── Reaction.java             # id, messageId, userId, emoji
│
├── attachment/
│   ├── controller/
│   │   └── AttachmentController.java # /api/files/**
│   ├── service/
│   │   └── AttachmentService.java
│   ├── repository/
│   │   └── AttachmentRepository.java
│   └── entity/
│       └── Attachment.java           # id, messageId, fileUrl, fileName, fileSize
│
├── presence/
│   ├── controller/
│   │   └── PresenceController.java   # @MessageMapping /app/presence/**
│   └── service/
│       └── PresenceService.java      # Redis 온라인 상태 관리
│
├── admin/
│   ├── controller/
│   │   └── AdminController.java      # /api/admin/**
│   └── service/
│       └── AdminService.java
│
└── common/
    ├── dto/
    │   └── ApiResponse.java          # { success, message, data } 공통 응답
    ├── exception/
    │   ├── CustomException.java
    │   ├── ErrorCode.java            # 에러 코드 enum
    │   └── GlobalExceptionHandler.java
    └── util/
        └── SecurityUtil.java         # 현재 로그인 사용자 ID 추출
```

---

## 8. 프론트엔드 컴포넌트 구조

```
frontend/src/
│
├── api/                              # Axios 기반 HTTP 요청 모듈
│   ├── axios.js                      # Axios 인스턴스 + 인터셉터 (401 처리, Token 갱신)
│   ├── auth.js                       # 로그인·로그아웃·회원가입·토큰 갱신
│   ├── channel.js                    # 채널 목록·생성·삭제
│   ├── message.js                    # 메시지 조회·수정·삭제
│   ├── dm.js                         # DM 방 목록·생성
│   ├── file.js                       # 파일 업로드
│   ├── reaction.js                   # 이모지 반응 추가·삭제
│   ├── user.js                       # 사용자 검색·AI 사용량 조회
│   └── admin.js                      # 관리자 API
│
├── socket/
│   └── stompClient.js                # STOMP 연결·구독·발행·재연결 로직
│
├── stores/                           # Pinia 전역 상태
│   ├── auth.js                       # 사용자 정보, Access Token, 로그인 여부
│   ├── channel.js                    # 채널 목록, 현재 채널, 안읽은 수
│   ├── message.js                    # 현재 채널 메시지 목록
│   ├── dm.js                         # DM 방 목록, 현재 DM
│   ├── presence.js                   # 사용자 온라인 상태 맵
│   └── ui.js                         # 토스트, 모달, 로딩 상태
│
├── router/
│   └── index.js                      # 라우트 정의 + 네비게이션 가드
│                                     # meta: { requiresAuth, requiresAdmin, guestOnly }
│
├── views/
│   ├── LoginView.vue
│   ├── JoinView.vue
│   ├── ChannelView.vue               # /channels/:channelId 메인 레이아웃
│   ├── DmView.vue                    # /dm/:dmRoomId
│   └── admin/
│       ├── AdminDashboardView.vue
│       ├── AdminChannelsView.vue
│       └── AdminUsersView.vue
│
└── components/
    ├── layout/
    │   ├── AppGnb.vue                # 상단 네비게이션 바
    │   ├── AppSidebar.vue            # 채널·DM 목록 사이드바
    │   └── ChannelHeader.vue         # 채널명·참여자 수·공지 배너
    │
    ├── chat/
    │   ├── MessageList.vue           # 메시지 목록 + 무한 스크롤
    │   ├── MessageItem.vue           # 개별 메시지 (텍스트·파일·AI 응답)
    │   ├── MessageInput.vue          # 입력창 + 파일 첨부 + 이모지 버튼
    │   ├── MessageEditInline.vue     # 인라인 수정 폼
    │   ├── TypingIndicator.vue       # "OOO님이 입력 중..." 표시
    │   ├── EmojiPicker.vue           # 이모지 선택 팝업
    │   ├── ReactionBar.vue           # 메시지 하단 이모지 반응 목록
    │   ├── FilePreview.vue           # 첨부파일 미리보기 (업로드 전)
    │   ├── ImagePreview.vue          # 이미지 인라인 표시
    │   └── NewMessageBadge.vue       # "N개의 새 메시지 ↓" 뱃지
    │
    ├── sidebar/
    │   ├── ChannelList.vue           # 채널 목록 + 안읽은 수 뱃지
    │   ├── ChannelItem.vue           # 채널 아이템
    │   ├── DmList.vue                # DM 목록
    │   ├── DmItem.vue                # DM 아이템 + 온라인 상태 점
    │   └── UserProfile.vue           # 하단 내 정보 영역
    │
    ├── common/
    │   ├── AppToast.vue              # 토스트 알림
    │   ├── AppModal.vue              # 범용 확인 모달
    │   ├── PresenceDot.vue           # 온라인 상태 점 (초록·노랑·회색)
    │   ├── UserAvatar.vue            # 아바타 이미지 또는 이니셜 원형
    │   └── UserSearchModal.vue       # DM 시작용 사용자 검색 모달
    │
    └── admin/
        ├── ChannelCreateModal.vue    # 채널 생성 모달
        └── UserTable.vue             # 회원 관리 테이블
```

---

## 9. 배포 구성

### 9.1 개발 환경 (로컬)

```
┌─────────────────────────────────────────────────────────┐
│  localhost:5173  ─── Vite Dev Server (Vue.js)           │
│       │                                                 │
│       │  proxy /api/** → localhost:8080                 │
│       │  proxy /ws/**  → localhost:8080                 │
│       ▼                                                 │
│  localhost:8080  ─── Spring Boot                        │
│  localhost:3306  ─── MySQL 8                            │
│  localhost:6379  ─── Redis 7                            │
└─────────────────────────────────────────────────────────┘

HTTPS: 개발 환경에서는 HTTP 사용 (localhost)
WSS: 개발 환경에서는 WS 사용
```

### 9.2 운영 환경 (배포 시)

```
┌─────────────────────────────────────────────────────────────┐
│  Internet                                                   │
│       │ HTTPS(443) / WSS                                    │
│       ▼                                                     │
│  Nginx (Reverse Proxy)                                      │
│  ├─ /          → Vue.js 빌드 정적 파일 서빙                 │
│  ├─ /api/**    → Spring Boot :8080 프록시                   │
│  ├─ /ws/**     → Spring Boot :8080 WebSocket 프록시         │
│  │             (upgrade: websocket 헤더 필수)               │
│  └─ SSL: Let's Encrypt (Certbot 자동 갱신)                  │
│       │                                                     │
│       ▼                                                     │
│  Spring Boot :8080                                          │
│  MySQL :3306                                                │
│  Redis :6379                                                │
└─────────────────────────────────────────────────────────────┘
```

### 9.3 환경별 WebSocket 엔드포인트

| 환경 | 프로토콜 | 엔드포인트 |
|------|----------|-----------|
| 개발 | WS | `ws://localhost:8080/ws` |
| 운영 | WSS | `wss://도메인/ws` |

> WSS는 HTTPS 위에서만 동작한다. Nginx SSL 설정 후 WebSocket Upgrade 헤더를 반드시 설정해야 한다.

```nginx
# Nginx WebSocket 프록시 설정 (운영 필수)
location /ws {
    proxy_pass http://localhost:8080;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_read_timeout 3600s;   # WebSocket 장기 연결 유지
}
```

---

## 10. 주요 설계 결정 사항

### 10.1 STOMP vs 순수 WebSocket
- **선택**: STOMP 프로토콜 채택
- **이유**: Spring 공식 지원, 채널별 Topic 구독 구조가 채팅 앱에 적합, SockJS 폴백으로 WebSocket 미지원 환경 대응

### 10.2 Redis Pub/Sub vs 인메모리 브로드캐스트
- **선택**: Redis Pub/Sub 채택
- **이유**: 단일 서버 MVP이지만 처음부터 Redis Pub/Sub 구조 적용. 향후 다중 인스턴스 수평 확장 시 코드 변경 없이 대응 가능

### 10.3 Access Token 저장 위치
- **선택**: 메모리 (Pinia store)
- **이유**: localStorage는 XSS 공격 시 탈취 가능. 메모리 저장 시 탭/새로고침 후 Refresh Token으로 자동 재발급하여 보완

### 10.4 메시지 소프트 삭제
- **선택**: `isDeleted` 플래그 방식 (소프트 삭제)
- **이유**: "삭제된 메시지입니다." 표시를 위해 레코드 유지 필요. 하드 삭제 시 이전 메시지 흐름이 끊어지는 UX 문제 발생

### 10.5 AI 응답 메시지 저장
- **선택**: DB에 영구 저장 (일반 메시지와 동일 테이블, type = AI_RESPONSE)
- **이유**: 채널 히스토리 로드 시 AI 응답도 함께 표시되어야 하므로 영속화 필요
