# 🔄 User Flow

## ChatterAI — 실시간 채팅 + AI 어시스턴트 플랫폼

> 이 문서는 각 기능의 상세 흐름과 분기 조건을 정의한다.  
> 구현 시 이 흐름을 기준으로 프론트엔드 분기 처리 및 백엔드 응답을 설계한다.

---

## 목차

1. [인증 흐름](#1-인증-흐름)
2. [채널 입장 흐름](#2-채널-입장-흐름)
3. [메시지 전송 흐름](#3-메시지-전송-흐름)
4. [@AI 멘션 흐름](#4-ai-멘션-흐름)
5. [파일 첨부 흐름](#5-파일-첨부-흐름)
6. [DM 흐름](#6-dm-흐름)
7. [메시지 수정·삭제 흐름](#7-메시지-수정삭제-흐름)
8. [이모지 반응 흐름](#8-이모지-반응-흐름)
9. [온라인 상태 흐름](#9-온라인-상태-흐름)
10. [Token 갱신 흐름](#10-token-갱신-흐름)
11. [관리자 흐름](#11-관리자-흐름)

---

## 1. 인증 흐름

### 1.1 회원가입

```
사용자 → /join 접근
  ├─ 이미 로그인 상태? → /channels 리다이렉트
  └─ 비로그인 상태 → 회원가입 폼 렌더링

폼 입력
  └─ 모든 필드 유효 → 가입 버튼 활성화
     └─ 가입 버튼 클릭
          └─ POST /api/auth/join
               ├─ 성공 (201)
               │    └─ 토스트 "가입이 완료되었습니다." → /login 이동
               ├─ 아이디 중복 (409 DUPLICATE_USERNAME)
               │    └─ 아이디 필드 하단 "이미 사용 중인 아이디입니다."
               ├─ 닉네임 중복 (409 DUPLICATE_NICKNAME)
               │    └─ 닉네임 필드 하단 "이미 사용 중인 닉네임입니다."
               └─ 유효성 오류 (400)
                    └─ 해당 필드 하단 에러 메시지
```

### 1.2 로그인

```
사용자 → /login 접근
  ├─ 이미 로그인 상태? → /channels 리다이렉트
  └─ 비로그인 상태 → 로그인 폼 렌더링

POST /api/auth/login
  ├─ 성공 (200)
  │    ├─ Access Token → Pinia authStore 저장 (메모리)
  │    ├─ Refresh Token → HttpOnly Cookie (서버가 Set-Cookie)
  │    ├─ 사용자 정보 (id, nickname, role) → Pinia authStore 저장
  │    └─ /channels 이동 → WebSocket 연결 시작
  ├─ 인증 실패 (401 LOGIN_FAILED)
  │    └─ 폼 하단 "아이디 또는 비밀번호가 올바르지 않습니다."
  └─ 비활성 계정 (403 ACCOUNT_DISABLED)
       └─ 폼 하단 "비활성화된 계정입니다. 관리자에게 문의해 주세요."
```

### 1.3 로그아웃

```
사용자 → GNB 프로필 클릭 → 로그아웃 버튼 클릭

POST /api/auth/logout
  ├─ 성공 여부와 무관하게
  │    ├─ WebSocket 연결 해제 (STOMP disconnect)
  │    ├─ Pinia authStore 초기화 (Access Token 삭제)
  │    ├─ Cookie 삭제 (서버가 Set-Cookie: refresh=; Max-Age=0)
  │    └─ /login 이동
  └─ 서버 오류 시에도 클라이언트 측 로그아웃 처리 동일하게 수행
```

---

## 2. 채널 입장 흐름

### 2.1 앱 최초 진입 (WebSocket 연결)

```
사용자 → /channels 접근
  ├─ 비로그인 상태? → /login 리다이렉트
  └─ 로그인 상태
       ├─ GET /api/channels (참여 채널 목록 로드)
       ├─ GET /api/dm/rooms (DM 방 목록 로드)
       ├─ WebSocket(STOMP) 연결 시작
       │    └─ CONNECT 프레임에 Authorization: Bearer {AccessToken}
       │         ├─ 연결 성공 → 각 채널/DM 구독 등록
       │         │    ├─ SUBSCRIBE /topic/channel/{channelId} (모든 참여 채널)
       │         │    ├─ SUBSCRIBE /topic/dm/{dmRoomId} (모든 DM 방)
       │         │    └─ SUBSCRIBE /user/queue/notification (개인 알림)
       │         └─ 연결 실패 (JWT 만료)
       │              └─ Token 갱신 흐름 진행 → 재연결 시도
       └─ 채널 목록의 첫 번째 채널로 자동 이동
```

### 2.2 채널 클릭

```
사이드바 채널 클릭 → /channels/:channelId 이동
  └─ GET /api/channels/{channelId}/messages?size=20 (최신 메시지 20개 로드)
       ├─ 성공 → 메시지 렌더링, 스크롤 최하단 이동
       │    └─ 해당 채널 안읽은 수 뱃지 → 0으로 초기화
       │         └─ DELETE /api/channels/{channelId}/unread (서버에 읽음 처리)
       └─ 404 (채널 없음) → 토스트 "채널을 찾을 수 없습니다." → 첫 채널로 이동
```

### 2.3 이전 메시지 무한 스크롤

```
사용자가 메시지 영역 최상단까지 스크롤
  └─ GET /api/channels/{channelId}/messages?before={oldest_message_id}&size=20
       ├─ 성공 → 기존 메시지 위에 추가 렌더링 (스크롤 위치 유지)
       ├─ 더 이상 메시지 없음 (빈 배열) → 로드 더 이상 시도하지 않음
       └─ 로딩 중 상단 스피너 표시
```

---

## 3. 메시지 전송 흐름

### 3.1 텍스트 메시지 전송

```
입력창에 텍스트 입력 후 Enter 또는 전송 버튼 클릭
  ├─ 빈 메시지? → 전송 무시
  ├─ @AI 포함? → AI 멘션 흐름으로 분기
  └─ 일반 텍스트
       ├─ [Optimistic UI] 메시지를 로컬 상태에 즉시 추가 (임시 ID 부여)
       └─ STOMP SEND /app/channel/{channelId}/send
            { content, type: "TEXT" }
            ├─ 서버 브로드캐스트 수신 (/topic/channel/{channelId})
            │    └─ 임시 ID → 서버 발급 실제 ID로 교체
            └─ 전송 실패 (연결 끊김 등)
                 └─ 임시 메시지 제거 + 토스트 "메시지 전송에 실패했습니다."
```

### 3.2 타이핑 인디케이터

```
입력창에 키 입력 감지
  └─ STOMP SEND /app/channel/{channelId}/typing
       { userId, nickname }

타이핑 이벤트 수신 (/topic/channel/{channelId}/typing)
  └─ 입력창 하단 "OOO님이 입력 중..." 표시
       └─ 마지막 수신 후 3초 경과 → 자동 사라짐

[주의] 본인이 보낸 타이핑 이벤트는 화면에 표시하지 않음
```

---

## 4. @AI 멘션 흐름

```
사용자가 "@AI [질문]" 입력 후 전송
  ├─ 비로그인? → 발생 불가 (채팅 접근 자체 차단)
  └─ 로그인 상태
       ├─ [프론트] 일일 AI 사용 횟수 확인
       │    └─ GET /api/users/me/ai-usage
       │         ├─ 잔여 횟수 > 0 → 계속
       │         └─ 잔여 횟수 = 0 (관리자 제외)
       │              └─ 토스트 "오늘의 AI 사용 횟수(20회)를 모두 사용했습니다." → 전송 중단
       │
       ├─ [1단계] 사용자 메시지 전송 (일반 메시지와 동일)
       │    └─ STOMP SEND /app/channel/{channelId}/send
       │         { content: "@AI 질문내용", type: "TEXT" }
       │
       ├─ [2단계] AI 로딩 메시지 수신
       │    └─ 서버가 브로드캐스트 (/topic/channel/{channelId})
       │         { type: "AI_LOADING", tempId: "...", content: "AI가 답변을 생성 중입니다..." }
       │         └─ 채널에 로딩 메시지 표시 (스피너 포함)
       │
       ├─ [3단계] AI 응답 완료 또는 실패
       │    └─ 서버가 브로드캐스트
       │         ├─ 성공: { type: "AI_RESPONSE", tempId: "...", content: "AI 응답 내용" }
       │         │    └─ 로딩 메시지 → 실제 AI 응답으로 교체
       │         └─ 실패: { type: "AI_ERROR", tempId: "..." }
       │              └─ 로딩 메시지 → "AI 응답에 실패했습니다. 잠시 후 다시 시도해 주세요."
       │
       └─ [4단계] AI 사용 횟수 차감 (서버 처리, 성공 시에만)
            └─ Redis ai:usage:{userId}:{date} INCR
```

---

## 5. 파일 첨부 흐름

```
입력창 클립 아이콘 클릭 → 파일 선택 다이얼로그
  └─ 파일 선택
       ├─ 개수 초과 (5개 초과) → 토스트 "파일은 최대 5개까지 첨부할 수 있습니다."
       ├─ 용량 초과 (이미지 10MB, 파일 20MB 초과)
       │    └─ 토스트 "OOO 파일이 용량 제한을 초과했습니다."
       └─ 유효한 파일
            ├─ 입력창 위에 첨부 파일 미리보기 표시
            │    ├─ 이미지: 썸네일 미리보기
            │    └─ 파일: 파일명 + 용량 표시
            └─ 전송 버튼 클릭
                 ├─ [1단계] POST /api/files/upload (Multipart)
                 │    ├─ 성공 → fileUrl[] 획득
                 │    └─ 실패 → 토스트 "파일 업로드에 실패했습니다."
                 └─ [2단계] STOMP SEND /app/channel/{channelId}/send
                      { content, type: "FILE", attachments: [{ fileUrl, fileName, fileSize }] }
```

---

## 6. DM 흐름

### 6.1 DM 방 생성

```
사이드바 DM 섹션 `+` 클릭 → 사용자 검색 모달 오픈
  └─ 닉네임 입력 (최소 1자)
       └─ GET /api/users/search?nickname={keyword}
            ├─ 결과 목록 표시 (닉네임 + 온라인 상태)
            └─ 사용자 선택
                 └─ POST /api/dm/rooms { targetUserId }
                      ├─ 신규 방 생성 (201) → /dm/{dmRoomId} 이동
                      │    └─ SUBSCRIBE /topic/dm/{dmRoomId}
                      └─ 기존 방 존재 (200) → 기존 /dm/{dmRoomId} 이동
```

### 6.2 DM 메시지 전송

```
(채널 메시지 전송 흐름과 동일, 경로만 다름)
  └─ STOMP SEND /app/dm/{dmRoomId}/send
       { content, type: "TEXT" | "FILE" }
```

---

## 7. 메시지 수정·삭제 흐름

### 7.1 메시지 수정 (본인만)

```
메시지 호버 → 더보기(⋯) → 수정 클릭
  ├─ AI 응답 메시지? → 수정 버튼 미노출 (렌더링 시 조건 처리)
  └─ 본인 메시지
       └─ 메시지 인라인 편집 모드 전환
            ├─ 내용 수정 후 Enter → PUT /api/messages/{messageId} { content }
            │    ├─ 성공 → 메시지 내용 업데이트 + "(수정됨)" 표시
            │    ├─ 403 FORBIDDEN → 토스트 "수정 권한이 없습니다."
            │    └─ 404 NOT_FOUND → 토스트 "메시지를 찾을 수 없습니다."
            └─ ESC → 편집 모드 취소
```

### 7.2 메시지 삭제

```
메시지 호버 → 더보기(⋯) → 삭제 클릭
  └─ 확인 모달 "메시지를 삭제할까요?" (취소 / 삭제)
       └─ 삭제 클릭
            └─ DELETE /api/messages/{messageId}
                 ├─ 성공 → 메시지 "삭제된 메시지입니다." 텍스트로 교체 (소프트 삭제)
                 │    └─ 해당 메시지의 첨부파일·이모지 반응도 숨김 처리
                 ├─ 403 FORBIDDEN → 토스트 "삭제 권한이 없습니다."
                 └─ 404 NOT_FOUND → 토스트 "메시지를 찾을 수 없습니다."

[삭제 권한]
  - 본인 메시지: 본인 + 관리자 가능
  - AI 응답 메시지: 관리자만 가능
  - 타인 메시지: 관리자만 가능
```

---

## 8. 이모지 반응 흐름

```
메시지 호버 → 😊 버튼 클릭 → 이모지 피커 팝업
  └─ 이모지 선택
       └─ POST /api/messages/{messageId}/reactions { emoji }
            ├─ 성공 (신규 반응 추가)
            │    └─ 메시지 하단 이모지 카운트 업데이트 (실시간 브로드캐스트)
            └─ 이미 반응한 이모지 선택 (409 REACTION_ALREADY_EXISTS)
                 └─ 자동으로 반응 취소 처리
                      └─ DELETE /api/messages/{messageId}/reactions { emoji }
                           └─ 메시지 하단 카운트 감소

메시지 하단 이모지 카운트 직접 클릭
  └─ (이모지 피커 없이) 해당 이모지 즉시 토글
       ├─ 미반응 상태 → POST (반응 추가)
       └─ 반응 상태 → DELETE (반응 취소)
```

---

## 9. 온라인 상태 흐름

### 9.1 온라인 상태 등록

```
WebSocket 연결 성공
  └─ 서버: Redis SET presence:{userId} "ONLINE" EX 60
       └─ 사이드바 내 해당 사용자 초록 점 표시

클라이언트 하트비트 (30초마다)
  └─ STOMP SEND /app/presence/heartbeat {}
       └─ 서버: Redis EXPIRE presence:{userId} 60 (TTL 갱신)
```

### 9.2 오프라인 처리

```
[정상 종료] 사용자 로그아웃 또는 탭 닫기 (beforeunload)
  └─ STOMP DISCONNECT 전송
       └─ 서버: Redis DEL presence:{userId} → 즉시 오프라인 처리

[비정상 종료] 네트워크 끊김, 브라우저 강제 종료
  └─ 하트비트 미수신으로 TTL(60초) 만료
       └─ 서버: 다음 presence 조회 시 키 없음 → 오프라인으로 반환

[자리비움] 마지막 활동으로부터 10분 경과
  └─ 클라이언트: 마우스/키보드 이벤트 감지 타이머
       └─ STOMP SEND /app/presence/away {}
            └─ 서버: Redis SET presence:{userId} "AWAY" EX 60
                 └─ 사이드바 노란 점으로 전환
```

---

## 10. Token 갱신 흐름

### 10.1 HTTP API 요청 중 만료

```
Axios 요청 → 401 Unauthorized 수신
  └─ Axios 인터셉터 처리
       └─ POST /api/auth/refresh (HttpOnly Cookie의 Refresh Token 자동 포함)
            ├─ 성공 (200)
            │    ├─ 새 Access Token → Pinia authStore 갱신
            │    └─ 원래 실패한 요청 재시도 (1회)
            └─ 실패 (401 REFRESH_TOKEN_EXPIRED or INVALID)
                 ├─ Pinia authStore 초기화
                 ├─ WebSocket 연결 해제
                 └─ /login?ret={현재경로} 이동
                      └─ 로그인 후 ret 경로로 복귀
```

### 10.2 WebSocket 연결 중 만료

```
STOMP ERROR 프레임 수신 (JWT 만료)
  └─ POST /api/auth/refresh
       ├─ 성공 → 새 Access Token으로 STOMP 재연결
       └─ 실패 → HTTP 만료 처리와 동일 (/login 이동)
```

---

## 11. 관리자 흐름

### 11.1 채널 생성

```
관리자 → 사이드바 채널 `+` 클릭 (관리자만 노출)
  └─ 채널 생성 모달
       ├─ 채널명 (필수), 설명 (선택), 공개/비공개 선택
       └─ 확인 클릭
            └─ POST /api/admin/channels { name, description, isPrivate }
                 ├─ 성공 → 사이드바 채널 목록에 추가, 새 채널로 이동
                 ├─ 중복 채널명 (409) → "이미 사용 중인 채널명입니다."
                 └─ 유효성 오류 (400) → 필드 하단 에러
```

### 11.2 채널 삭제

```
관리자 → /admin/channels → 채널 선택 → 삭제 버튼
  └─ 확인 모달 "채널을 삭제하면 모든 메시지와 파일이 삭제됩니다. 계속할까요?"
       └─ 삭제 클릭
            └─ DELETE /api/admin/channels/{channelId}
                 ├─ 성공
                 │    ├─ 해당 채널 메시지·첨부파일 전체 삭제
                 │    ├─ 해당 채널 구독자에게 브로드캐스트 { type: "CHANNEL_DELETED" }
                 │    │    └─ 구독자들: 채널 목록에서 제거 + 첫 번째 채널로 이동
                 │    └─ 사이드바 채널 목록 갱신
                 └─ 404 → 토스트 "채널을 찾을 수 없습니다."
```

### 11.3 공지 메시지 지정

```
관리자 → 채널 내 메시지 호버 → 더보기(⋯) → 공지로 지정
  └─ POST /api/admin/channels/{channelId}/notice { messageId }
       ├─ 성공
       │    ├─ 채널 헤더 아래 공지 배너로 해당 메시지 표시
       │    └─ 채널 구독자 전체에게 브로드캐스트 { type: "NOTICE_UPDATED" }
       └─ 이미 지정된 공지 → 기존 공지 해제 후 새 공지 지정
```

### 11.4 회원 비활성화

```
관리자 → /admin/users → 회원 선택 → 비활성화 버튼
  └─ 확인 모달
       └─ PUT /api/admin/users/{userId}/disable
            ├─ 성공
            │    ├─ Redis의 해당 사용자 Refresh Token 삭제 (강제 로그아웃)
            │    ├─ 해당 사용자 WebSocket 연결 강제 종료 (STOMP ERROR 전송)
            │    └─ 관리자 화면: 해당 회원 상태 "비활성" 표시
            └─ 자기 자신 비활성화 시도 → 토스트 "본인 계정은 비활성화할 수 없습니다."
```
