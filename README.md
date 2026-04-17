# ChatterAI

**채널 기반 실시간 채팅 + AI 어시스턴트 협업 플랫폼**

> Claude Code(AI 코딩 에이전트)를 활용한 **AI-assisted 풀스택 프로젝트**
> 단순한 "AI가 코드를 써줬다"가 아닌, **기획 → 설계 → 구현** 전 과정을 AI와 협업하여 완성한 프로젝트입니다.

---

## 프로젝트 소개

Slack에서 영감을 받아 만든 채팅 협업 플랫폼입니다.
채널 기반 실시간 메시지 전달, 1:1 DM, 그리고 채팅창에서 바로 `@AI`를 멘션하면 Claude AI가 대화 맥락을 파악해 답변해주는 기능이 핵심입니다.

```
사용자: @AI 지금까지 논의한 내용을 정리해줘
ChatterAI: 채팅 히스토리를 분석한 결과... (AI 자동 응답)
```

---

## AI 활용 개발 방식 (핵심 어필 포인트)

이 프로젝트에서 가장 강조하고 싶은 부분은 **코드 자체가 아니라, AI를 도구로 활용하는 능력**입니다.

### 1. 체계적인 스펙 문서 주도 개발

코드를 한 줄 작성하기 전에 **7개의 설계 문서**를 먼저 완성하고, 이를 AI에게 컨텍스트로 제공해 구현했습니다.

```
docs/
├── prd.md          # 제품 요구사항 정의 (기능 명세, 권한 매트릭스)
├── user-flow.md    # 상세 유저 플로우 (분기 조건, 엣지 케이스)
├── architecture.md # 시스템 구조, 인증 흐름, 데이터 흐름도
├── db-schema.md    # 테이블 설계, 인덱스 전략
├── api-spec.md     # REST + WebSocket STOMP 전체 API 명세
├── ui-spec.md      # 페이지 레이아웃, 컴포넌트 조건
└── error-spec.md   # 에러 코드 목록, 프론트 처리 방식
```

> "어떻게 만들지"보다 **"무엇을 만들지"를 먼저 정의**하고, AI가 그 스펙을 따르도록 지시한 것이 핵심입니다.

### 2. 단계별 구현 계획 (plan.md)

무작정 구현하지 않고 **17개의 Phase**로 작업을 분할하고, 의존성 순서를 설계했습니다.

```
Phase 1  DB 스키마 · 프로젝트 초기화
Phase 2  백엔드 공통 기반 (ApiResponse, ErrorCode, Security)
Phase 3  인증 (JWT + Redis Refresh Token)
Phase 4  채널 · 메시지 REST API
Phase 5  WebSocket (STOMP) 실시간 채팅
Phase 6  Redis Pub/Sub 메시지 브로드캐스트
Phase 7  AI 멘션 (@AI → Claude API)
Phase 8  DM (1:1 다이렉트 메시지)
Phase 9  파일 첨부 · 이모지 반응 · 관리자 API
Phase 10-17  프론트엔드 (공통 기반 → 인증 → WebSocket → 채팅 → DM → 관리자 → 통합 테스트)
```

> 각 Phase가 완료되어야 다음 Phase로 진행하는 규칙을 두어 AI가 충돌 없는 코드를 생성하도록 통제했습니다.

### 3. CLAUDE.md를 통한 AI 행동 규칙 정의

프로젝트 루트에 `CLAUDE.md`를 두어 AI가 코드를 작성할 때 **반드시 따라야 할 규칙**을 명시했습니다.

- 공통 응답 형식 (`ApiResponse`) 강제
- Redis Pub/Sub를 통한 브로드캐스트 (SimpMessagingTemplate 직접 호출 금지)
- Access Token 메모리 저장, localStorage 사용 금지
- 소프트 삭제 강제 (물리 삭제 금지)
- 에러 처리 패턴 통일 (`CustomException → GlobalExceptionHandler`)

> AI에게 자유를 주는 것이 아니라 **일관성 있는 아키텍처를 유지하도록 제약**을 설계한 점이 핵심입니다.

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| Frontend | Vue.js 3 (Composition API), Vite, Pinia, Vue Router 4, Axios |
| Backend | Spring Boot 3, Spring Security, JPA |
| 실시간 | WebSocket (STOMP over SockJS), Redis Pub/Sub |
| 인증 | JWT (Access Token 메모리 + Refresh Token HttpOnly Cookie) |
| AI | Anthropic Claude API (claude-sonnet-4-5) |
| DB | MySQL 8 |
| 캐시/메시지 | Redis 7 |

---

## 주요 기능

### 실시간 채팅
- 채널 기반 그룹 채팅 (WebSocket STOMP)
- 타이핑 인디케이터 ("OOO님이 입력 중...")
- 메시지 Optimistic UI (전송 즉시 로컬 표시 → 서버 확정 후 교체)
- 무한 스크롤 (커서 기반 페이지네이션)

### AI 어시스턴트
- `@AI` 멘션으로 Claude AI 호출
- 채널 최근 10개 메시지를 컨텍스트로 전달 (대화 맥락 파악)
- AI 로딩 → 응답 교체 흐름 (Optimistic UI)
- 일반 회원 일일 20회 제한 (Redis TTL 기반)

### 보안
- JWT Access Token (30분) + Refresh Token (7일, HttpOnly Cookie)
- Access Token 만료 시 자동 갱신 후 원래 요청 재시도
- STOMP 연결 시 JWT 검증 (`StompAuthChannelInterceptor`)
- XSS 방지: localStorage 사용 금지, 메모리 토큰 저장

### DM (1:1 다이렉트 메시지)
- 사용자 검색 → DM 방 생성 또는 기존 방 입장
- DM에서도 `@AI` 멘션 가능

### 온라인 상태
- Redis TTL(60초) 기반 하트비트
- 온라인 / 자리비움(10분 미활동) / 오프라인 상태 표시

### 관리자 기능
- 채널 생성·삭제 (삭제 시 실시간 브로드캐스트)
- 공지 메시지 지정 (채널 헤더 고정 배너)
- 회원 비활성화 (Redis Refresh Token 즉시 삭제 → 강제 로그아웃)

---

## 아키텍처

### 메시지 흐름 (Redis Pub/Sub)

```
사용자 A (STOMP SEND)
    │
    ▼
Spring Boot (메시지 DB 저장)
    │
    ▼
Redis PUBLISH (chat:channel:{id})
    │
    ├──▶ 구독자 1 (Spring Boot 인스턴스 1) ──▶ 사용자 A 수신
    └──▶ 구독자 2 (Spring Boot 인스턴스 2) ──▶ 사용자 B, C 수신
```

> 단일 서버 MVP지만 처음부터 Redis Pub/Sub 구조로 설계 → **수평 확장 시 코드 변경 없음**

### @AI 멘션 처리 흐름

```
메시지 전송(@AI 감지)
    │
    ├──▶ 일반 메시지 DB 저장 + 브로드캐스트
    │
    ├──▶ AI_LOADING 이벤트 브로드캐스트 (tempId 포함)
    │         → 클라이언트: "AI가 답변 생성 중..." 표시
    │
    ├──▶ Claude API 호출 (최근 10개 메시지 컨텍스트)
    │
    └──▶ AI_RESPONSE 브로드캐스트 (tempId로 로딩 메시지 교체)
              → 클라이언트: AI 응답으로 교체 표시
```

### 인증 흐름

```
로그인
    ▼
Access Token (30분, 메모리 저장)
Refresh Token (7일, HttpOnly Cookie)
    │
API 요청 시 ──▶ 401 수신 ──▶ Refresh Token으로 재발급 ──▶ 원래 요청 재시도
                              ──▶ 재발급 실패 ──▶ 로그아웃 + /login?ret=...
```

---

## 실행 방법

### 사전 요구사항

| 도구 | 버전 |
|------|------|
| Java | 17 이상 |
| Node.js | 18 이상 |
| MySQL | 8.x |
| Redis | 7.x |

### 환경 구성 (최초 1회)

```bash
# 1. DB 스키마 생성
mysql -u root -p chatterai < db-schema.sql

# 2. 초기 데이터 삽입 (관리자 계정 + 기본 채널)
mysql -u root -p chatterai < db-seed.sql

# 3. 백엔드 환경 파일 생성
# backend/src/main/resources/application-local.yml
# DB 비밀번호, JWT Secret, Claude API Key 입력

# 4. 프론트엔드 의존성 설치
cd frontend && npm install
```

### 실행

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

> 기본 관리자 계정: `admin` / `Admin1234!`

---

## 프로젝트 구조

```
chatterai/
├── backend/                          # Spring Boot 백엔드
│   └── src/main/java/com/chatterai/
│       ├── config/                   # Security, WebSocket, Redis, CORS, Claude API
│       ├── auth/                     # 인증 (JWT, Filter, Interceptor)
│       ├── channel/                  # 채널 CRUD
│       ├── message/                  # 메시지 REST + STOMP + AI
│       ├── dm/                       # 1:1 DM
│       ├── reaction/                 # 이모지 반응
│       ├── attachment/               # 파일 첨부
│       ├── presence/                 # 온라인 상태 (Redis TTL)
│       ├── admin/                    # 관리자 API
│       └── common/                   # ApiResponse, ErrorCode, GlobalExceptionHandler
│
├── frontend/                         # Vue.js 3 프론트엔드
│   └── src/
│       ├── api/                      # Axios 인스턴스 + 도메인별 API 모듈
│       ├── socket/                   # STOMP 클라이언트 (연결, 구독, 재연결)
│       ├── stores/                   # Pinia 전역 상태 (auth, channel, message, dm, ui)
│       ├── router/                   # 라우트 + 네비게이션 가드
│       ├── views/                    # 페이지 컴포넌트
│       └── components/               # 재사용 UI 컴포넌트
│
├── docs/                             # 7개 설계 문서 (구현 전 작성)
├── CLAUDE.md                         # AI 코딩 에이전트 행동 규칙
└── plan.md                           # 17개 Phase 구현 체크리스트
```

---

## 이 프로젝트에서 배운 것

### AI 협업에서 가장 중요한 것: 명확한 지시

AI가 코드를 잘 작성하게 하려면 **개발자가 먼저 설계를 완성해야 합니다.**
막연하게 "채팅 앱 만들어줘"가 아니라:

- "채널 메시지 브로드캐스트는 Redis Pub/Sub를 통해서만 처리한다. SimpMessagingTemplate 직접 호출은 금지다."
- "Access Token은 메모리에만 저장한다. localStorage 사용 시 보안 이슈를 명시하고 금지한다."
- "401이 두 번 연속 오면 authStore를 초기화하고 /login?ret=... 으로 이동한다."

이런 **구체적이고 이유가 있는 제약** 덕분에 일관성 있는 코드가 나왔습니다.

### AI는 "구현" 도구, 설계는 개발자 몫

7개의 설계 문서를 직접 작성하며 비로소:
- WebSocket STOMP 인증이 HTTP 필터와 다른 경로를 탄다는 것
- Redis Pub/Sub 구조가 왜 수평 확장에 필요한지
- Access Token과 Refresh Token의 역할 분리와 보안 trade-off

를 깊이 이해하게 됐습니다. AI는 그 이해를 코드로 옮기는 도구였습니다.

---

## 향후 개선 계획

- [ ] Elasticsearch 기반 메시지 검색
- [ ] OAuth2 소셜 로그인
- [ ] 메시지 스레드 (Slack의 Reply 기능)
- [ ] FCM 푸시 알림
- [ ] Docker Compose 기반 원클릭 실행 환경

---

## 관련 문서

| 문서 | 내용 |
|------|------|
| [PRD](docs/prd.md) | 기능 명세, 권한 매트릭스 |
| [Architecture](docs/architecture.md) | 시스템 구조, 인증·메시지·AI 흐름도 |
| [API Spec](docs/api-spec.md) | REST + WebSocket STOMP 전체 API |
| [DB Schema](docs/db-schema.md) | 테이블 설계, DDL |
| [Error Spec](docs/error-spec.md) | 에러 코드 목록, 프론트 처리 방식 |
| [Plan](plan.md) | 17개 Phase 구현 체크리스트 |
