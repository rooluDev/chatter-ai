# 📋 plan.md

## ChatterAI — 구현 체크리스트

> 작업 시작 전 현재 완료된 Phase를 확인한다.  
> 각 항목이 완료되면 체크박스를 체크한다.  
> Phase는 순서대로 진행한다. 이전 Phase가 완료되지 않으면 다음 Phase로 넘어가지 않는다.

---

## 진행 현황

```
Phase 1  [x] 프로젝트 초기화
Phase 2  [x] DB 설정
Phase 3  [x] 백엔드 공통 기반
Phase 4  [x] 백엔드 인증
Phase 5  [x] 백엔드 채널 · 메시지
Phase 6  [x] 백엔드 WebSocket (STOMP)
Phase 7  [x] 백엔드 Redis 연동
Phase 8  [x] 백엔드 AI 멘션
Phase 9  [x] 백엔드 DM
Phase 10 [x] 백엔드 파일 · 이모지 · 관리자
Phase 11 [x] 프론트엔드 공통 기반
Phase 12 [x] 프론트엔드 인증
Phase 13 [x] 프론트엔드 WebSocket 클라이언트
Phase 14 [x] 프론트엔드 채널 채팅
Phase 15 [x] 프론트엔드 DM
Phase 16 [x] 프론트엔드 관리자
Phase 17 [x] 통합 확인
```

---

## Phase 1 — 프로젝트 초기화

### 1.1 백엔드 (Spring Boot)

- [x] `backend/` 디렉토리에 Spring Initializr 프로젝트 생성
  - Group: `com.chatterai`
  - Artifact: `chatterai`
  - Java 17, Spring Boot 3.x
  - Dependencies: Spring Web, Spring Security, Spring Data JPA, Spring WebSocket, Spring Data Redis, MySQL Driver, Lombok, Validation
- [x] `build.gradle`에 추가 의존성 등록
  - `jjwt-api`, `jjwt-impl`, `jjwt-jackson` (JWT)
  - `spring-messaging` (STOMP)
- [x] `application.yml` 생성 (`env-spec.md` 2.1 기준)
- [x] `application-local.yml` 생성 (`env-spec.md` 2.2 기준, Git 제외 확인)
- [x] `.gitignore` 설정 (`application-local.yml`, `uploads/` 포함)
- [x] 패키지 구조 생성 (`architecture.md` 7절 기준)
  - `config/`, `auth/`, `user/`, `channel/`, `message/`, `dm/`, `reaction/`, `attachment/`, `presence/`, `admin/`, `common/`

### 1.2 프론트엔드 (Vue.js)

- [x] `frontend/` 디렉토리에 Vite + Vue3 프로젝트 생성
  ```bash
  npm create vite@latest frontend -- --template vue
  ```
- [x] 의존성 설치
  ```bash
  npm install vue-router@4 pinia axios @stomp/stompjs sockjs-client
  ```
- [x] `.env` 생성 (`env-spec.md` 3.1 기준)
- [x] `vite.config.js` 프록시 설정 (`env-spec.md` 3.3 기준, `ws: true` 확인)
- [x] `.gitignore` 설정
- [x] 디렉토리 구조 생성
  - `src/api/`, `src/socket/`, `src/stores/`, `src/router/`, `src/views/`, `src/components/`

---

## Phase 2 — DB 설정

- [x] MySQL에 `chatterai` 데이터베이스 생성
  ```sql
  CREATE DATABASE chatterai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  ```
- [x] `db-schema.sql` 작성 및 실행 (`db-schema.md` 4절 DDL 기준)
  - 테이블 생성 순서 준수: `users` → `channels` → `channel_participants` → `dm_rooms` → `dm_participants` → `messages` → `channels` ALTER (notice FK) → `attachments` → `reactions`
- [x] `db-seed.sql` 작성 및 실행 (`db-schema.md` 5절 기준)
  - 관리자 계정 (`admin` / `Admin1234!`)
  - 기본 채널 3개 (`일반`, `공지`, `자유`)
- [x] Spring Boot 기동 후 JPA `ddl-auto: validate` 통과 확인

---

## Phase 3 — 백엔드 공통 기반

### 3.1 공통 응답 형식

- [x] `ApiResponse<T>` 클래스 작성
  ```java
  { "success": true/false, "message": "...", "data": T }
  ```
- [x] `ApiResponse.success()`, `ApiResponse.error()` 정적 팩토리 메서드 작성

### 3.2 에러 처리

- [x] `ErrorCode` enum 작성 (`error-spec.md` 2.1 전체 목록 기준)
- [x] `CustomException` 클래스 작성
- [x] `GlobalExceptionHandler` 작성 (`error-spec.md` 2.3 기준)
  - `CustomException` 처리
  - `MethodArgumentNotValidException` 처리
  - `AccessDeniedException` 처리
  - `Exception` (공통) 처리

### 3.3 Security 기반 설정

- [x] `SecurityConfig` 작성 (permitAll / hasAnyRole 경로 설정)
- [x] `CorsConfig` 작성 (SecurityConfig에 `CorsConfigurationSource` 빈 등록)
  - `OPTIONS` preflight 허용 확인

### 3.4 유틸리티

- [x] `SecurityUtil.getCurrentUserId()` 작성 (SecurityContext에서 userId 추출)

---

## Phase 4 — 백엔드 인증

### 4.1 User Entity · Repository

- [x] `User` Entity 작성 (`id`, `username`, `nickname`, `password`, `role`, `isActive`, `createdAt`, `updatedAt`)
- [x] `Role` enum 작성 (`USER`, `ADMIN`)
- [x] `UserRepository` 작성 (`findByUsername`, `findByNickname`)

### 4.2 JWT

- [x] `JwtProvider` 작성
  - `generateAccessToken(userId, role)` → 30분 만료
  - `generateRefreshToken()` → UUID
  - `validateToken(token)` → boolean
  - `getUserIdFromToken(token)` → Long
- [x] `JwtAuthenticationFilter` 작성 (HTTP 요청 JWT 검증 → SecurityContext 설정)

### 4.3 Redis — Refresh Token 저장소

- [x] `RedisConfig` 작성 (`RedisTemplate<String, String>` 빈 등록)
- [x] `RefreshTokenRepository` 작성 (Redis CRUD: `refresh:{userId}` 키, TTL 7일)

### 4.4 Auth API

- [x] `JoinRequestDto` 작성 (username, password, nickname + @Valid 검증)
- [x] `LoginRequestDto`, `LoginResponseDto` 작성
- [x] `AuthService` 작성
  - `join()`: 중복 검증 → BCrypt 암호화 → DB 저장
  - `login()`: 사용자 조회 → 비밀번호 검증 → Access/Refresh Token 발급 → Redis 저장
  - `refresh()`: Cookie에서 Refresh Token 추출 → Redis 검증 → 새 Access Token 발급
  - `logout()`: Redis Refresh Token 삭제 → Cookie 삭제
- [x] `AuthController` 작성 (`/api/auth/join`, `/api/auth/login`, `/api/auth/refresh`, `/api/auth/logout`)
- [x] **검증**: Postman 또는 curl로 회원가입 → 로그인 → 토큰 갱신 → 로그아웃 흐름 확인

---

## Phase 5 — 백엔드 채널 · 메시지 (REST)

### 5.1 Channel

- [x] `Channel` Entity 작성
- [x] `ChannelParticipant` Entity 작성
- [x] `ChannelRepository`, `ChannelParticipantRepository` 작성
- [x] `ChannelResponseDto` 작성 (id, name, description, isPrivate, unreadCount, lastMessage)
- [x] `ChannelService` 작성
  - `getMyChannels(userId)`: 참여 채널 목록 + 안읽은 수 (Redis)
  - `getChannelDetail(channelId, userId)`: 채널 상세
  - `joinChannel(channelId, userId)`: 채널 참여
  - `markAsRead(channelId, userId)`: 읽음 처리 (Redis unread 카운트 0)
- [x] `ChannelController` 작성 (`/api/channels/**`)

### 5.2 Message (REST 부분)

- [x] `Message` Entity 작성 (`channel_id`, `dm_room_id`, `user_id`, `content`, `type`, `isDeleted`, `isAiMessage`)
- [x] `MessageType` enum 작성 (`TEXT`, `FILE`, `AI_RESPONSE`, `AI_ERROR`)
- [x] `MessageRepository` 작성
  - `findByChannelIdWithCursor(channelId, beforeId, size)`: 커서 기반 페이지네이션
  - `findByDmRoomIdWithCursor(dmRoomId, beforeId, size)`: 커서 기반 페이지네이션
- [x] `MessageResponseDto` 작성 (id, userId, nickname, content, type, isDeleted, isAiMessage, isOwn, attachments, reactions, createdAt, updatedAt)
- [x] `MessageService` 작성
  - `getChannelMessages(channelId, userId, beforeId, size)`: 채널 메시지 목록
  - `updateMessage(messageId, userId, content)`: 수정 (권한 검증 포함)
  - `deleteMessage(messageId, userId)`: 소프트 삭제 (권한 검증 포함)
- [x] `MessageController` 작성 (`PUT /api/messages/{id}`, `DELETE /api/messages/{id}`)
- [x] **검증**: 채널 메시지 조회 (커서 기반), 수정, 삭제 API 동작 확인

---

## Phase 6 — 백엔드 WebSocket (STOMP)

### 6.1 WebSocket 설정

- [x] `WebSocketConfig` 작성
  - STOMP 엔드포인트: `/ws` (SockJS 폴백 포함)
  - 메시지 브로커: `/topic`, `/user/queue`
  - 애플리케이션 목적지 접두사: `/app`
- [x] `StompAuthChannelInterceptor` 작성
  - CONNECT 프레임에서 `Authorization` 헤더 추출 → JWT 검증 → SecurityContext 설정
  - 토큰 없거나 만료 시 `MessageDeliveryException` 던져 STOMP ERROR 프레임 반환

### 6.2 채널 메시지 WebSocket

- [x] `ChatMessageRequestDto` 작성 (content, type, attachments)
- [x] `ChatMessageResponseDto` 작성 (eventType + message 포함)
- [x] `ChatMessageController` 작성 (`@MessageMapping("/channel/{channelId}/send")`)
  - 메시지 DB 저장 → Redis Pub/Sub PUBLISH → `/topic/channel/{channelId}` 브로드캐스트
- [x] 타이핑 인디케이터 처리 (`@MessageMapping("/channel/{channelId}/typing")`)
  - `/topic/channel/{channelId}/typing` 브로드캐스트 (DB 저장 없음)
- [x] **검증**: SockJS info endpoint 응답 확인, Redis Pub/Sub 패턴 구독 확인, 채널 메시지 조회 확인

---

## Phase 7 — 백엔드 Redis 연동

- [x] `RedisMessagePublisher` 작성 (채널/DM 메시지 PUBLISH)
- [x] `RedisMessageSubscriber` 작성 (SUBSCRIBE 수신 → STOMP 브로드캐스트)
- [x] `RedisConfig`에 `MessageListenerContainer` 빈 등록 (채널·DM 패턴 구독)
- [x] `PresenceService` 작성
  - `setOnline(userId)`: `presence:{userId}` SET, TTL 60초
  - `setAway(userId)`: `presence:{userId}` SET "AWAY", TTL 60초
  - `setOffline(userId)`: `presence:{userId}` DEL
  - `isOnline(userId)`: `presence:{userId}` GET
- [x] `PresenceController` 작성 (`@MessageMapping("/presence/heartbeat")`, `@MessageMapping("/presence/away")`)
- [x] 안읽은 메시지 수 Redis 처리
  - 메시지 저장 시: 채널 참여자 중 현재 채널 미열람 사용자의 `unread:{userId}:{channelId}` INCR
  - 읽음 처리 시: `unread:{userId}:{channelId}` DEL
- [x] `NotificationService` 작성 — user-targeted UNREAD_COUNT 알림 (`/user/queue/notification`)
- [ ] **검증**: 온라인 상태 점 변경, 안읽은 수 뱃지 동작 확인

---

## Phase 8 — 백엔드 AI 멘션

- [x] `ClaudeApiConfig` 작성 (Anthropic API 클라이언트 설정, 타임아웃 10초)
- [x] AI 사용량 Redis 처리
  - `ai:usage:{userId}:{date}` INCR (TTL 24시간)
  - `checkUsage(userId)`: 잔여 횟수 확인
  - `incrementUsage(userId)`: 사용량 증가
- [x] `AiMessageService` 작성
  - `processAiMentionAsync(channelId, userId, question, tempId)`:
    1. 사용량 검증 (`AI_USAGE_EXCEEDED` 체크)
    2. 채널 최근 메시지 10개 조회 (컨텍스트 구성)
    3. AI 로딩 메시지 브로드캐스트 (`AI_LOADING` 이벤트, tempId 포함)
    4. Anthropic API 호출 (claude-sonnet-4-5, 타임아웃 10초)
    5. 성공: AI 응답 DB 저장 → `AI_RESPONSE` 브로드캐스트 (tempId로 로딩 메시지 교체)
    6. 실패/타임아웃: `AI_ERROR` 브로드캐스트
    7. 성공 시 사용량 INCR
- [x] `ChatMessageController`에 `@AI` 멘션 감지 분기 추가
- [x] `GET /api/users/me/ai-usage` 엔드포인트 추가 (UserController)
- [x] **검증**: AI usage API 응답 확인 완료

---

## Phase 9 — 백엔드 DM

- [x] `DmRoom` Entity, `DmParticipant` Entity 작성
- [x] `DmRoomRepository`, `DmParticipantRepository` 작성
- [x] `DmService` 작성
  - `getMyDmRooms(userId)`: 내 DM 방 목록 (상대방 정보 + 온라인 상태 + 안읽은 수)
  - `getOrCreateDmRoom(userId, targetUserId)`: DM 방 생성 또는 기존 방 반환
  - `getDmMessages(dmRoomId, userId, beforeId, size)`: DM 메시지 목록 (권한 검증 포함)
  - `markDmAsRead(dmRoomId, userId)`: DM 읽음 처리
- [x] `DmController` 작성 (`/api/dm/**`)
- [x] `DmMessageController` 작성 (`@MessageMapping("/dm/{dmRoomId}/send")`)
  - DM 메시지 DB 저장 → Redis Pub/Sub PUBLISH → `/topic/dm/{dmRoomId}` 브로드캐스트
  - `@AI` 멘션 감지 → `AiMessageService` 호출 (DM에서도 AI 사용 가능)
- [ ] **검증**: 두 사용자 간 DM 방 생성 → 메시지 전송 → 양쪽 수신 확인

---

## Phase 10 — 백엔드 파일 · 이모지 · 관리자

### 10.1 파일 첨부

- [x] `Attachment` Entity 작성
- [x] `AttachmentRepository`, `AttachmentService` 작성
- [x] `AttachmentController` 작성 (`POST /api/files/upload`, `GET /api/files/uploads/**`)
  - 파일 개수 (5개) / 크기 (이미지 10MB, 파일 20MB) / 확장자 검증
  - 저장 경로: `./uploads/chat/{date}/` , 파일명: `{UUID}-{originalName}`
  - 정적 리소스 핸들러 등록 (`/api/files/uploads/**` → 로컬 디렉토리)

### 10.2 이모지 반응

- [x] `Reaction` Entity 작성
- [x] `ReactionRepository`, `ReactionService` 작성
  - 반응 추가: UNIQUE 제약으로 중복 방지 → 성공 시 해당 채널 `REACTION_UPDATED` 브로드캐스트
  - 반응 취소: 삭제 후 `REACTION_UPDATED` 브로드캐스트
- [x] `ReactionController` 작성 (`POST/DELETE /api/messages/{id}/reactions`)

### 10.3 관리자 API

- [x] `AdminService` 작성
  - `createChannel()`, `deleteChannel()`: 채널 생성/삭제 (삭제 시 구독자에게 `CHANNEL_DELETED` 브로드캐스트)
  - `setNoticeMessage()`, `removeNoticeMessage()`: 공지 설정/해제 (`NOTICE_UPDATED` 브로드캐스트)
  - `getUsers()`, `disableUser()`, `enableUser()`: 회원 관리 (비활성화 시 Redis Refresh Token 삭제)
- [x] `AdminController` 작성 (`/api/admin/**`)
- [ ] **검증**: 파일 업로드 → 메시지에 첨부 → 이모지 반응 → 관리자 채널 생성 확인

---

## Phase 11 — 프론트엔드 공통 기반

### 11.1 Axios 설정

- [x] `api/axios.js` 작성
  - 기본 URL: `VITE_API_BASE_URL`
  - 요청 인터셉터: `Authorization: Bearer {accessToken}` 헤더 자동 추가
  - 응답 인터셉터: 401 → Refresh Token 갱신 재시도 → 실패 시 로그아웃 + `/login?ret=...`
  - 403 → 토스트 / 404 → 토스트 / 429 → 토스트 / 500+ → 토스트
  - 네트워크 오류 → 토스트 "네트워크 연결을 확인해 주세요."

### 11.2 Pinia 스토어

- [x] `stores/auth.js`: userId, nickname, role, accessToken, isLoggedIn, isAdmin
- [x] `stores/channel.js`: 채널 목록, 현재 채널 ID, 안읽은 수 맵
- [x] `stores/message.js`: 현재 채널 메시지 목록, hasNext, 로딩 상태
- [x] `stores/dm.js`: DM 방 목록, 현재 DM 방 ID
- [x] `stores/presence.js`: `{ [userId]: 'ONLINE' | 'AWAY' | 'OFFLINE' }` 맵
- [x] `stores/ui.js`: 토스트 큐, 모달 상태, 전역 로딩

### 11.3 Router

- [x] `router/index.js` 작성
  - 라우트 정의: `/`, `/login`, `/join`, `/channels/:channelId`, `/dm/:dmRoomId`, `/admin/**`
  - `meta: { requiresAuth }` → 비로그인 시 `/login?ret=...` 이동
  - `meta: { requiresAdmin }` → ADMIN 아닌 경우 `/channels`로 리다이렉트
  - `meta: { guestOnly }` → 로그인 상태면 `/channels`로 리다이렉트

### 11.4 API 모듈

- [x] `api/auth.js`: join, login, refresh, logout
- [x] `api/channel.js`: getChannels, getChannelDetail, getChannelMessages, markAsRead
- [x] `api/message.js`: updateMessage, deleteMessage
- [x] `api/dm.js`: getDmRooms, createOrGetDmRoom, getDmMessages, markDmAsRead
- [x] `api/file.js`: upload
- [x] `api/reaction.js`: addReaction, removeReaction
- [x] `api/user.js`: getMe, searchUsers, getAiUsage
- [x] `api/admin.js`: createChannel, deleteChannel, setNotice, removeNotice, getUsers, disableUser, enableUser

### 11.5 공통 컴포넌트

- [x] `components/common/AppToast.vue`: 토스트 알림 (success/error/info, 3초 자동 소멸)
- [x] `components/common/AppModal.vue`: 확인 모달 (제목, 설명, 취소/확인 버튼)
- [x] `components/common/UserAvatar.vue`: 아바타 이미지 또는 이니셜 원형
- [x] `components/common/PresenceDot.vue`: 온라인 상태 점

---

## Phase 12 — 프론트엔드 인증

- [x] `views/LoginView.vue` 작성
  - 로그인 폼 → 성공 시 authStore 저장 → `/channels` 이동
  - 401/403 에러 → 폼 하단 에러 메시지
  - `guestOnly` 가드 적용
- [x] `views/JoinView.vue` 작성
  - 회원가입 폼 → 실시간 유효성 검사 → 모든 필드 유효 시 버튼 활성화
  - 409 에러 → 해당 필드 하단 에러 메시지
  - `guestOnly` 가드 적용
- [x] `components/layout/AppGnb.vue` 작성
  - 프로필 드롭다운: 닉네임·역할 표시 + 로그아웃 버튼
  - 로그아웃: authStore 초기화 → WebSocket 해제 → `/login` 이동
- [x] **검증**: 빌드 성공 확인

---

## Phase 13 — 프론트엔드 WebSocket 클라이언트

- [x] `socket/stompClient.js` 작성
  - `connect(accessToken)`: STOMP 연결, `Authorization` 헤더 포함
  - `disconnect()`: 연결 해제
  - `subscribeChannel(channelId)`: `/topic/channel/{channelId}` 구독
  - `unsubscribeChannel(channelId)`: 구독 해제
  - `subscribeDm(dmRoomId)`: `/topic/dm/{dmRoomId}` 구독
  - `subscribeNotification()`: `/user/queue/notification` 구독
  - `sendChannelMessage(channelId, payload)`: `/app/channel/{channelId}/send`
  - `sendDmMessage(dmRoomId, payload)`: `/app/dm/{dmRoomId}/send`
  - `sendTyping(channelId)`: `/app/channel/{channelId}/typing`
  - `sendHeartbeat()`: `/app/presence/heartbeat`
  - `onStompError`: JWT 만료 시 토큰 갱신 후 재연결
  - `onDisconnect`: 재연결 전략 (즉시 → 3초 → 5초, `error-spec.md` 5.2 기준)
- [x] 하트비트 인터벌 등록 (30초마다 `sendHeartbeat()`)
- [x] **검증**: 빌드 성공 확인

---

## Phase 14 — 프론트엔드 채널 채팅

### 14.1 레이아웃

- [x] `components/layout/AppSidebar.vue` 작성
- [x] `components/sidebar/ChannelItem.vue`: 채널명, 안읽은 수 뱃지, 활성 스타일
- [x] `components/sidebar/UserProfile.vue`: 아바타, 닉네임, 온라인 상태 점, 설정 드롭다운
- [x] `views/ChannelView.vue` 작성 (전체 레이아웃 틀)

### 14.2 채널 헤더 · 메시지 목록

- [x] `components/layout/ChannelHeader.vue`: 채널명, 참여자 수, 공지 배너
- [x] `components/chat/MessageList.vue`
- [x] `components/chat/MessageItem.vue`
- [x] `components/chat/NewMessageBadge.vue`
- [x] `components/chat/ReactionBar.vue`

### 14.3 메시지 입력

- [x] `components/chat/MessageInput.vue`
- [x] `components/chat/FilePreview.vue`
- [x] `components/chat/EmojiPicker.vue`
- [x] `components/chat/MessageEditInline.vue`
- [x] `components/chat/TypingIndicator.vue`

### 14.4 WebSocket 이벤트 처리

- [x] CHAT / AI_LOADING / AI_RESPONSE / AI_ERROR / MESSAGE_UPDATED / MESSAGE_DELETED
- [x] REACTION_UPDATED / NOTICE_UPDATED / CHANNEL_DELETED / UNREAD_COUNT / TYPING
- [x] **검증**: 빌드 성공 확인

---

## Phase 15 — 프론트엔드 DM

- [x] `components/sidebar/DmList.vue`, `DmItem.vue`: DM 목록, 상대방 온라인 상태
- [x] `components/common/UserSearchModal.vue`: 사용자 검색 → 선택 → DM 방 생성
- [x] `views/DmView.vue` 작성
  - 채널 채팅과 동일 구조 (MessageList, MessageInput 재사용)
  - 헤더: 상대방 닉네임 + 온라인 상태 점
- [x] DM WebSocket 구독 및 이벤트 처리 (채널과 동일 구조)
- [x] **검증**: 빌드 성공 확인

---

## Phase 16 — 프론트엔드 관리자

- [x] `router/index.js` 중첩 라우트로 재구성 (`/admin/dashboard`, `/admin/channels`, `/admin/users`)
- [x] `views/admin/AdminView.vue`: 관리자 사이드 메뉴 레이아웃 + RouterView
- [x] `views/admin/AdminDashboardView.vue`: 채널 수, 회원 수, 활성 회원 수 통계
- [x] `views/admin/AdminChannelsView.vue`: 채널 목록 테이블, 생성/삭제
- [x] `components/admin/ChannelCreateModal.vue`: 채널 생성 모달 (이름, 설명, 비공개 토글, 409 처리)
- [x] `views/admin/AdminUsersView.vue`: 회원 목록, 닉네임 검색, 페이지네이션, 비활성화/활성화
- [x] **검증**: 빌드 성공 확인

---

## Phase 17 — 통합 확인

### 17.1 인증 흐름 전체

- [ ] 회원가입 → 로그인 → Access Token 만료 시 자동 갱신 → 로그아웃
- [ ] 비로그인 상태에서 `/channels` 직접 접근 → `/login?ret=/channels` 리다이렉트
- [ ] 로그인 후 `/login` 접근 → `/channels` 리다이렉트

### 17.2 채팅 핵심 흐름

- [ ] 두 브라우저 탭(A, B)에서 같은 채널 입장 → A가 전송 → B에 즉시 수신
- [ ] `@AI` 멘션 → 로딩 메시지 표시 → AI 응답 교체 (A, B 모두 확인)
- [ ] 메시지 수정 → 양쪽 실시간 반영
- [ ] 메시지 삭제 → "삭제된 메시지입니다." 양쪽 표시
- [ ] 이모지 반응 → 양쪽 카운트 실시간 업데이트
- [ ] 타이핑 인디케이터 → 3초 후 자동 사라짐

### 17.3 DM 흐름

- [ ] A → B에게 DM 시작 → B 사이드바에 DM 방 생성
- [ ] DM 양방향 메시지 전송 확인
- [ ] DM에서 `@AI` 멘션 동작 확인

### 17.4 AI 사용량 제한

- [ ] 일반 회원 20회 초과 시 토스트 표시 + 전송 차단
- [ ] 관리자는 제한 없이 AI 사용 가능

### 17.5 관리자 기능

- [ ] 채널 생성 → 일반 회원 사이드바 즉시 반영
- [ ] 채널 삭제 → 해당 채널 접속 중인 사용자 → 첫 채널로 이동
- [ ] 회원 비활성화 → 해당 사용자 자동 로그아웃 (강제 토큰 무효화)
- [ ] 공지 메시지 지정 → 채널 헤더 공지 배너 즉시 반영

### 17.6 온라인 상태

- [ ] 로그인 → 사이드바 초록 점
- [ ] 10분 미활동 → 노란 점 (자리비움)
- [ ] 로그아웃 → 회색 점 (오프라인)
- [ ] 브라우저 강제 종료 → 60초 후 오프라인 처리

### 17.7 에러 케이스

- [ ] 파일 5개 초과 첨부 → 토스트 에러
- [ ] 서버 다운 상태에서 메시지 전송 → Optimistic UI 롤백 + 토스트
- [ ] WebSocket 연결 끊김 → 재연결 시도 → 3회 실패 시 새로고침 안내
- [ ] AI API 타임아웃 (10초) → 에러 메시지로 교체

### 17.8 보안

- [ ] JWT 없이 API 호출 → 401 반환 확인
- [ ] 일반 회원으로 `/api/admin/**` 호출 → 403 반환 확인
- [ ] 타인 메시지 수정/삭제 시도 → 403 반환 확인
- [ ] 만료된 Access Token으로 API 호출 → 자동 갱신 후 재시도 확인
