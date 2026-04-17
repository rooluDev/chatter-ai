# CLAUDE.md

## ChatterAI — 실시간 채팅 + AI 어시스턴트 플랫폼

> 이 파일은 Claude Code가 매 세션 시작 시 가장 먼저 읽는 프로젝트 마스터 가이드다.
> 코드를 작성하기 전에 이 파일을 반드시 끝까지 읽고 숙지한 후 작업을 시작한다.

---

## 1. 프로젝트 개요

채널 기반 실시간 채팅 + AI 어시스턴트 협업 플랫폼.
WebSocket(STOMP) + Redis Pub/Sub 기반 실시간 메시지 전달,
HTTPS/WSS 보안 통신, MCP 연동 AI 봇 응답을 핵심 기술로 한다.

| 항목 | 내용 |
|------|------|
| Frontend | Vue.js 3 (Composition API), Vite, Pinia, Vue Router 4, Axios |
| Backend | Spring Boot 3, Spring Security, JPA, MySQL 8 |
| 실시간 | WebSocket (STOMP over SockJS), Redis Pub/Sub |
| 인증 | JWT (Access Token 메모리 + Refresh Token HttpOnly Cookie) |
| AI | Anthropic Claude API (claude-sonnet-4-5) |
| 파일 스토리지 | 로컬 파일 시스템 (`./uploads/`) |
| 관리자 계정 | DB 직접 삽입 (별도 가입 없음) |

---

## 2. 프로젝트 구조

```
chatterai/
├── backend/                          # Spring Boot 백엔드
│   └── src/main/java/com/chatterai/
│       ├── config/                   # SecurityConfig, WebSocketConfig, RedisConfig, CorsConfig, ClaudeApiConfig
│       ├── auth/                     # 인증 (controller, service, dto, jwt)
│       ├── user/                     # User entity, Role enum
│       ├── channel/                  # 채널 (controller, service, repository, entity, dto)
│       ├── message/                  # 메시지 REST + STOMP (ChatMessageController 포함)
│       ├── dm/                       # DM (controller, service, repository, entity)
│       ├── reaction/                 # 이모지 반응
│       ├── attachment/               # 파일 첨부
│       ├── presence/                 # 온라인 상태 (Redis)
│       ├── admin/                    # 관리자 API
│       └── common/                   # ApiResponse, ErrorCode, CustomException, GlobalExceptionHandler, SecurityUtil
│   └── src/main/resources/
│       ├── application.yml           # 공통 설정 (Git O)
│       └── application-local.yml     # DB·JWT·Claude API Key (Git X)
│
├── frontend/                         # Vue.js 3 프론트엔드
│   └── src/
│       ├── api/                      # axios.js + 도메인별 API 모듈
│       ├── socket/                   # stompClient.js
│       ├── stores/                   # auth, channel, message, dm, presence, ui (Pinia)
│       ├── router/                   # index.js (라우트 + 네비게이션 가드)
│       ├── views/                    # 페이지 컴포넌트 (LoginView, JoinView, ChannelView, DmView, admin/)
│       └── components/               # layout/, chat/, sidebar/, common/, admin/
│
├── docs/                             # 설계 문서
│   ├── prd.md
│   ├── user-flow.md
│   ├── architecture.md
│   ├── db-schema.md
│   ├── api-spec.md
│   ├── ui-spec.md
│   ├── error-spec.md
│   └── env-spec.md
├── plan.md                           # 구현 체크리스트
├── db-schema.sql
└── db-seed.sql
```

---

## 3. 개발 환경 실행

### 사전 요구사항

| 도구 | 버전 |
|------|------|
| Java | 17 이상 |
| Node.js | 18 이상 |
| MySQL | 8.x |
| Redis | 7.x |

### 실행 명령어

```bash
# 터미널 1 — Redis
redis-server

# 터미널 2 — 백엔드
cd backend
./gradlew bootRun

# 터미널 3 — 프론트엔드
cd frontend
npm run dev
```

| 서비스 | URL |
|--------|-----|
| Frontend | http://localhost:5173 |
| Backend API | http://localhost:8080/api |
| WebSocket | ws://localhost:8080/ws |
| MySQL | localhost:3306/chatterai |
| Redis | localhost:6379 |

### 최초 환경 구성 (한 번만)

```bash
# 1. DB 스키마 생성
mysql -u root -p chatterai < db-schema.sql

# 2. 초기 데이터 삽입 (관리자 계정 + 기본 채널)
mysql -u root -p chatterai < db-seed.sql

# 3. 백엔드 환경 파일 생성 (직접 편집)
# backend/src/main/resources/application-local.yml
# → DB 비밀번호, JWT Secret, Claude API Key 입력 (env-spec.md 2.2 참조)

# 4. 프론트엔드 의존성 설치
cd frontend && npm install
```

---

## 4. 핵심 설계 규칙

### 4.1 공통 응답 형식 (절대 변경 금지)

모든 REST API 응답은 아래 구조를 따른다. 예외 없음.

```json
{ "success": true,  "message": "...", "data": { ... } }
{ "success": false, "message": "...", "data": null }
```

채팅 메시지 페이지네이션 응답의 `data` 구조 (커서 기반):

```json
{
  "content": [...],
  "hasNext": true,
  "nextCursorId": 42,
  "size": 20
}
```

> 채팅 메시지는 `pageNum` 방식이 아닌 `before={messageId}` 커서 기반 페이지네이션을 사용한다.

### 4.2 에러 처리 원칙

- **백엔드**: `CustomException(ErrorCode)` throw → `GlobalExceptionHandler`가 공통 응답 반환
- **프론트엔드 401 (첫 번째)**: Refresh Token으로 Access Token 재발급 → 원래 요청 재시도
- **프론트엔드 401 (재시도 후 실패)**: authStore 초기화 + WebSocket 해제 + `/login?ret=...` 이동
- **프론트엔드 403**: 토스트 에러 표시
- **프론트엔드 404**: 토스트 에러 표시
- **프론트엔드 429**: 토스트 에러 표시
- **프론트엔드 500+**: 토스트 에러 표시
- **409 (중복 에러)**: 인터셉터에서 토스트 미처리. 컴포넌트에서 필드별 에러 메시지 표시
- 서버 내부 정보(스택 트레이스, SQL)는 절대 응답에 포함하지 않는다

### 4.3 인증 / 권한

- **Access Token**: 메모리(Pinia authStore)에 저장. 새로고침 시 사라지나 Refresh Token으로 자동 재발급
- **Refresh Token**: HttpOnly Cookie. XSS 공격 시 탈취 불가. 만료 7일
- **localStorage 사용 금지**: 보안 이슈. Access Token은 반드시 메모리에만 저장
- JWT 만료: Access Token 30분, Refresh Token 7일
- 권한 3단계: `비회원` → `USER` → `ADMIN`
- Spring Security `permitAll` / `hasAnyRole(USER,ADMIN)` / `hasRole(ADMIN)` 3계층
- Service 레이어에서 추가 검증: 본인 메시지 확인, DM 방 참여 여부

### 4.4 CORS + Spring Security 설정 (필수)

Spring Security 필터가 `WebMvcConfigurer`보다 먼저 실행된다.
`CorsConfig`(WebMvcConfigurer)만으로는 preflight `OPTIONS` 요청이 차단된다.
`SecurityConfig`에 반드시 `CorsConfigurationSource` 빈을 등록해야 한다.

```java
// SecurityConfig.java
http
    .cors(cors -> cors.configurationSource(corsConfigurationSource()))
    .csrf(AbstractHttpConfigurer::disable)
    ...

@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of(corsProperties.getAllowedOrigins()));
    config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);  // Cookie 전송을 위해 필수
    config.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
}
```

> `allowCredentials(true)` 누락 시 Refresh Token Cookie가 전달되지 않는다.

### 4.5 WebSocket STOMP 인증 (필수)

HTTP 요청은 `JwtAuthenticationFilter`가 처리하지만,
STOMP 연결은 HTTP 필터 체인을 거치지 않는다.
`StompAuthChannelInterceptor`를 반드시 구현해 CONNECT 프레임의 JWT를 검증해야 한다.

```java
// StompAuthChannelInterceptor.java
@Override
public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
        String token = accessor.getFirstNativeHeader("Authorization");
        // "Bearer " 제거 후 JWT 검증
        // SecurityContext에 Authentication 설정
    }
    return message;
}
```

> 이 인터셉터 없으면 인증 없이 WebSocket 연결 가능 → 보안 구멍 발생

### 4.6 Vite 프록시 설정 (필수)

`ws: true` 옵션 없으면 개발 환경에서 STOMP 연결이 실패한다.
파일 서빙 프록시 없으면 이미지가 깨진다.

```javascript
// vite.config.js
proxy: {
  '/api': { target: 'http://localhost:8080', changeOrigin: true },
  '/ws':  { target: 'http://localhost:8080', changeOrigin: true, ws: true },
}
```

### 4.7 Redis Pub/Sub 구조

메시지 브로드캐스트는 `SimpMessagingTemplate.convertAndSend()` 직접 호출이 아닌
Redis Pub/Sub를 통해 처리한다.

```
메시지 수신 → DB 저장 → Redis PUBLISH → Redis SUBSCRIBE → STOMP 브로드캐스트
```

단일 서버 MVP이지만 처음부터 이 구조로 구현해야 향후 다중 인스턴스 확장 시 코드 변경이 없다.

| Key 패턴 | 용도 | TTL |
|----------|------|-----|
| `chat:channel:{channelId}` | 채널 메시지 Pub/Sub 토픽 | 없음 |
| `chat:dm:{dmRoomId}` | DM 메시지 Pub/Sub 토픽 | 없음 |
| `presence:{userId}` | 온라인 상태 | 60초 |
| `unread:{userId}:{channelId}` | 안읽은 메시지 수 | 없음 |
| `refresh:{userId}` | Refresh Token | 7일 |
| `ai:usage:{userId}:{date}` | AI 일일 사용량 | 24시간 |

### 4.8 파일 업로드

| 구분 | 최대 개수 | 파일당 최대 | 허용 확장자 |
|------|-----------|-------------|-------------|
| 채팅 첨부 (이미지) | 5개 | 10MB | jpg, jpeg, png, gif, webp |
| 채팅 첨부 (파일) | 5개 | 20MB | pdf, zip, txt, md |

- 저장 경로: `./uploads/chat/{date}/`
- 저장 파일명: `{UUID}-{originalName}` (중복 방지)
- 파일 서빙: Spring 정적 리소스 핸들러 (`/api/files/uploads/**` → 로컬 디렉토리)
- 파일 업로드는 **2단계**: `POST /api/files/upload`로 URL 먼저 획득 → STOMP 메시지에 URL 포함
- 파일 IO 오류는 예외를 삼키고 로그만 기록 (메시지 트랜잭션에 영향 주지 않음)

### 4.9 메시지 소프트 삭제

메시지 삭제는 `is_deleted = 1`로 처리한다. 물리 삭제 금지.
- 응답 시 `is_deleted = true`이면 `content`를 `"삭제된 메시지입니다."`로 교체
- 삭제된 메시지의 `attachments`, `reactions`는 빈 배열로 반환

### 4.10 AI 메시지 처리

- AI 응답은 `user_id = NULL`, `is_ai_message = 1`로 DB 저장
- `AI_LOADING` 타입은 DB에 저장하지 않는다 (브로드캐스트 전용)
- AI 응답 메시지는 수정 불가, 관리자만 삭제 가능
- Claude API 호출 타임아웃: 10초
- 일반 회원 일일 제한: 20회 (`ai:usage:{userId}:{date}`, TTL 24h)
- 이중 검증: 프론트(1차) + 서버(2차) 모두 사용량 확인

---

## 5. 백엔드 코딩 컨벤션

### 5.1 계층별 책임

```
Controller  → 요청 수신, 응답 반환. 비즈니스 로직 없음
Service     → 비즈니스 로직, 권한 검사, 트랜잭션
Repository  → DB 쿼리 (JPA / JPQL)
Entity      → DB 매핑 전용. 비즈니스 로직 없음
DTO         → 계층 간 데이터 전달. Entity 직접 노출 금지
```

### 5.2 네이밍 규칙

| 대상 | 규칙 | 예시 |
|------|------|------|
| Entity | 도메인명 단수 | `Channel`, `Message`, `DmRoom` |
| Repository | `{Entity}Repository` | `ChannelRepository` |
| Service | `{Domain}Service` | `ChannelService` |
| Controller (REST) | `{Domain}Controller` | `ChannelController` |
| Controller (STOMP) | `{Domain}MessageController` | `ChatMessageController` |
| DTO (요청) | `{Domain}RequestDto` | `ChatSendRequestDto` |
| DTO (응답) | `{Domain}ResponseDto` | `ChatMessageResponseDto` |
| REST API 경로 | `/api/{domain}` (소문자) | `/api/channels`, `/api/dm` |
| STOMP 수신 경로 | `/app/{domain}/{id}/action` | `/app/channel/{channelId}/send` |
| STOMP 발행 경로 | `/topic/{domain}/{id}` | `/topic/channel/{channelId}` |

### 5.3 Service 레이어 패턴

```java
// 권한 검증은 반드시 Service에서
public void deleteMessage(Long messageId, Long currentUserId) {
    Message message = messageRepository.findById(messageId)
        .orElseThrow(() -> new CustomException(ErrorCode.MESSAGE_NOT_FOUND));

    // AI 메시지는 일반 사용자 삭제 불가 (관리자는 별도 경로)
    boolean isOwner = message.getUserId() != null
        && message.getUserId().equals(currentUserId);
    if (!isOwner) {
        throw new CustomException(ErrorCode.FORBIDDEN);
    }

    message.softDelete();  // is_deleted = true
    messageRepository.save(message);
}
```

### 5.4 WebSocket 브로드캐스트 패턴

```java
// Redis Pub/Sub를 통한 브로드캐스트 (직접 SimpMessagingTemplate 사용 금지)
redisMessagePublisher.publish(
    "chat:channel:" + channelId,
    chatMessageResponseDto
);
```

---

## 6. 프론트엔드 코딩 컨벤션

### 6.1 Composition API 사용

모든 컴포넌트는 `<script setup>` 사용. Options API 사용 금지.

```vue
<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
</script>
```

### 6.2 API 호출 패턴

```javascript
// try-catch로 에러 처리, 인터셉터가 공통 처리 후 컴포넌트에서 추가 처리
const fetchMessages = async () => {
  isLoading.value = true
  try {
    const { data } = await channelApi.getMessages(channelId, { size: 20 })
    messages.value = data.content
    hasNext.value = data.hasNext
    nextCursorId.value = data.nextCursorId
  } catch (error) {
    // 403, 404, 500은 인터셉터에서 토스트 처리됨
    // 400, 409는 여기서 직접 처리
  } finally {
    isLoading.value = false
  }
}
```

### 6.3 스토어 사용 패턴

```javascript
import { useAuthStore }    from '@/stores/auth'
import { useChannelStore } from '@/stores/channel'
import { useUiStore }      from '@/stores/ui'

const authStore    = useAuthStore()
const channelStore = useChannelStore()
const uiStore      = useUiStore()

// 권한 확인
if (!authStore.isLoggedIn) { ... }
if (authStore.isAdmin) { ... }

// 토스트
uiStore.showToast('success', '메시지가 전송되었습니다.')
uiStore.showToast('error',   '전송에 실패했습니다.')
uiStore.showToast('info',    'AI 사용량이 초과되었습니다.')

// 확인 모달
uiStore.openModal({
  title: '메시지 삭제',
  message: '메시지를 삭제할까요?',
  onConfirm: () => handleDelete()
})
```

### 6.4 WebSocket 이벤트 처리 패턴

```javascript
// ChannelView.vue
onMounted(() => {
  stompClient.subscribeChannel(channelId, (event) => {
    switch (event.eventType) {
      case 'CHAT':             messageStore.addMessage(event.message);       break
      case 'AI_LOADING':       messageStore.addLoadingMessage(event);        break
      case 'AI_RESPONSE':      messageStore.replaceLoadingMessage(event);    break
      case 'AI_ERROR':         messageStore.replaceLoadingMessage(event);    break
      case 'MESSAGE_UPDATED':  messageStore.updateMessage(event.message);    break
      case 'MESSAGE_DELETED':  messageStore.softDeleteMessage(event.messageId); break
      case 'REACTION_UPDATED': messageStore.updateReactions(event);          break
      case 'NOTICE_UPDATED':   channelStore.updateNotice(event);             break
      case 'CHANNEL_DELETED':  handleChannelDeleted(event.channelId);        break
    }
  })
})

onUnmounted(() => {
  stompClient.unsubscribeChannel(channelId)
})
```

### 6.5 네이밍 규칙

| 대상 | 규칙 | 예시 |
|------|------|------|
| 컴포넌트 파일 | PascalCase | `MessageItem.vue` |
| 뷰 파일 | `{Name}View.vue` | `ChannelView.vue` |
| 스토어 | camelCase | `useAuthStore` |
| API 모듈 | camelCase | `channelApi.getMessages()` |
| 변수·함수 | camelCase | `fetchMessages()`, `isLoading` |
| 이벤트 핸들러 | `handle{Action}` | `handleSend()`, `handleDelete()` |

### 6.6 라우터 가드 meta 3종

```javascript
meta: { requiresAuth: true }   // 비로그인이면 /login?ret=... 이동
meta: { requiresAdmin: true }  // ADMIN 아니면 /channels로 리다이렉트
meta: { guestOnly: true }      // 이미 로그인이면 /channels로 리다이렉트
```

---

## 7. 주요 비즈니스 규칙 (구현 시 반드시 준수)

### 7.1 메시지 묶음 (isFirstInGroup)

```
이전 메시지와 userId가 같고 시간 차이가 5분 이내 → isFirstInGroup = false (아바타/닉네임 생략)
이전 메시지와 userId가 다르거나 5분 초과 → isFirstInGroup = true
```

프론트엔드에서 메시지 목록 렌더링 시 계산한다. 백엔드 응답에는 포함하지 않는다.

### 7.2 @AI 멘션 이중 검증

```
프론트 1차: GET /api/users/me/ai-usage → remainCount <= 0이면 전송 중단
서버 2차: ai:usage 카운트 확인 → 초과 시 AI_USAGE_EXCEEDED (429) 반환
```

클라이언트 우회 시도를 서버에서 반드시 차단한다.

### 7.3 Optimistic UI (메시지 전송)

```
1. 메시지를 로컬 상태에 즉시 추가 (임시 ID 부여, 전송 중 스타일)
2. STOMP SEND 발행
3. 서버 브로드캐스트 수신 → 임시 ID를 실제 서버 ID로 교체
4. 실패 시 → 임시 메시지 제거 + 토스트
```

### 7.4 WebSocket 재연결 전략

```
연결 끊김 → 즉시 재시도 → 3초 후 재시도 → 5초 후 재시도
→ 3회 모두 실패 → 토스트 "연결이 끊어졌습니다. 페이지를 새로 고침해 주세요."
```

### 7.5 온라인 상태 (Redis TTL 방식)

```
하트비트 (30초마다): presence:{userId} TTL 갱신 (60초)
정상 종료: STOMP DISCONNECT → presence:{userId} DEL
비정상 종료: TTL 60초 만료 → 자동 오프라인 처리 (별도 구현 불필요)
자리비움: 마지막 활동 10분 경과 → presence:{userId} = "AWAY"
```

### 7.6 채널 메시지 커서 기반 페이지네이션

```
최초 로드: GET /api/channels/{id}/messages?size=20
           → 최신 20개 반환, nextCursorId 포함

무한 스크롤: GET /api/channels/{id}/messages?size=20&before={nextCursorId}
             → 해당 ID보다 이전 20개 반환
             → hasNext = false이면 더 이상 로드하지 않음
```

### 7.7 DM 방 중복 생성 방지

```
POST /api/dm/rooms { targetUserId }
→ 두 사용자 간 이미 DM 방이 존재하면 201이 아닌 200으로 기존 방 반환
→ 프론트에서 200/201 구분 없이 동일하게 처리 (반환된 dmRoomId로 이동)
```

### 7.8 채널 삭제 시 구독자 처리

```
관리자가 채널 삭제
→ 해당 채널 구독자 전체에게 CHANNEL_DELETED 이벤트 브로드캐스트
→ 프론트: 채널 목록에서 제거 + 현재 그 채널이면 첫 번째 채널로 자동 이동
```

### 7.9 AI 로딩 메시지 처리

```
AI_LOADING 브로드캐스트: tempId 포함, DB 저장 없음
AI_RESPONSE 브로드캐스트: 동일 tempId로 로딩 메시지 교체, DB 저장 있음
AI_ERROR 브로드캐스트: 동일 tempId로 에러 메시지 교체, DB 저장 있음
```

프론트에서 `tempId`를 키로 로딩 메시지를 찾아 교체한다.

---

## 8. 주요 에러 코드 (빠른 참조)

| ErrorCode | HTTP | 사용 상황 |
|-----------|------|-----------|
| `INVALID_INPUT` | 400 | @Valid 실패, 필수값 누락 |
| `LOGIN_FAILED` | 401 | 로그인 실패 |
| `UNAUTHORIZED` | 401 | JWT 없음/만료 |
| `REFRESH_TOKEN_EXPIRED` | 401 | Refresh Token 만료 |
| `FORBIDDEN` | 403 | 권한 없음 |
| `ACCOUNT_DISABLED` | 403 | 비활성 계정 |
| `USER_NOT_FOUND` | 404 | 사용자 없음 |
| `CHANNEL_NOT_FOUND` | 404 | 채널 없음 |
| `MESSAGE_NOT_FOUND` | 404 | 메시지 없음 |
| `DM_ROOM_NOT_FOUND` | 404 | DM 방 없음 |
| `DUPLICATE_USERNAME` | 409 | 아이디 중복 |
| `DUPLICATE_NICKNAME` | 409 | 닉네임 중복 |
| `DUPLICATE_CHANNEL_NAME` | 409 | 채널명 중복 |
| `REACTION_ALREADY_EXISTS` | 409 | 이모지 중복 반응 |
| `AI_USAGE_EXCEEDED` | 429 | AI 일일 한도 초과 |
| `AI_API_ERROR` | 502 | Claude API 에러 |
| `AI_API_TIMEOUT` | 504 | Claude API 타임아웃 |
| `SERVER_ERROR` | 500 | 예상치 못한 예외 |

전체 에러 코드는 `docs/error-spec.md` 4절 참조.

---

## 9. 작업 전 체크리스트

새 작업을 시작하기 전에 아래를 확인한다.

- [ ] `plan.md`에서 현재 작업할 Phase와 항목을 확인했는가?
- [ ] 해당 기능의 API 스펙을 `docs/api-spec.md`에서 확인했는가?
- [ ] WebSocket 관련이면 STOMP 경로를 `docs/api-spec.md` 9절에서 확인했는가?
- [ ] 해당 페이지의 UI 레이아웃과 버튼 조건을 `docs/ui-spec.md`에서 확인했는가?
- [ ] 에러 케이스를 `docs/error-spec.md`에서 확인했는가?
- [ ] 백엔드라면: Controller → Service → Repository → Entity → DTO 순서로 작성하는가?
- [ ] 프론트엔드라면: API 모듈 → Store → 컴포넌트 → 뷰 순서로 작성하는가?
- [ ] 공통 응답 형식 `ApiResponse`를 사용하는가?
- [ ] 에러 발생 시 `CustomException(ErrorCode)`를 throw하는가?
- [ ] 메시지 브로드캐스트는 Redis Pub/Sub를 통해 처리하는가? (SimpMessagingTemplate 직접 호출 금지)
- [ ] AI 메시지는 `user_id = NULL`, `is_ai_message = 1`로 저장하는가?
- [ ] 파일 IO 오류는 예외를 삼키고 로그만 기록하는가?
- [ ] `application-local.yml`이 `.gitignore`에 포함되어 있는가?

---

## 10. 스펙 문서 빠른 참조

| 궁금한 내용 | 참조 문서 |
|-------------|-----------|
| 기능 명세, 권한 매트릭스 | `docs/prd.md` |
| 기능별 상세 흐름, 분기 조건 | `docs/user-flow.md` |
| 패키지 구조, 컴포넌트 구조, 배포 구성 | `docs/architecture.md` |
| 테이블 컬럼, DDL, 인덱스 | `docs/db-schema.md` |
| REST API 엔드포인트, 요청/응답 필드 | `docs/api-spec.md` |
| WebSocket STOMP 경로, 이벤트 타입 | `docs/api-spec.md` 9절 |
| 페이지 레이아웃, 버튼 표시 조건 | `docs/ui-spec.md` |
| 에러 코드 전체 목록, 프론트 처리 방식 | `docs/error-spec.md` |
| 환경 변수, 설정 파일 | `docs/env-spec.md` |
| 구현 순서, 체크리스트 | `plan.md` |
