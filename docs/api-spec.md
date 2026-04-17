# 📡 API Spec

## ChatterAI — 실시간 채팅 + AI 어시스턴트 플랫폼

> 이 문서는 전체 REST API 엔드포인트와 WebSocket STOMP 메시지 명세를 정의한다.  
> 구현 시 이 문서의 요청·응답 필드를 기준으로 Controller · DTO를 작성한다.

---

## 공통 규칙

### 공통 응답 형식

모든 REST API 응답은 아래 구조를 따른다. 예외 없음.

```json
// 성공
{ "success": true,  "message": "처리되었습니다.", "data": { ... } }

// 실패
{ "success": false, "message": "에러 메시지",     "data": null }
```

### 페이지네이션 응답 (data 내부)

```json
{
  "content": [ ... ],
  "hasNext": true,
  "nextCursorId": 42,
  "size": 20
}
```

> 채팅 메시지는 페이지 번호 방식이 아닌 **커서 기반 페이지네이션**을 사용한다.  
> `before={messageId}` 파라미터로 해당 ID 이전 메시지를 가져온다.

### 인증 헤더

```
Authorization: Bearer {accessToken}
```

권한이 필요한 모든 API에 포함. 없거나 만료 시 `401` 반환.

### Base URL

```
개발: http://localhost:8080/api
운영: https://{도메인}/api
```

---

## 목차

1. [인증 API](#1-인증-api)
2. [사용자 API](#2-사용자-api)
3. [채널 API](#3-채널-api)
4. [메시지 API](#4-메시지-api)
5. [DM API](#5-dm-api)
6. [파일 API](#6-파일-api)
7. [이모지 반응 API](#7-이모지-반응-api)
8. [관리자 API](#8-관리자-api)
9. [WebSocket STOMP 명세](#9-websocket-stomp-명세)

---

## 1. 인증 API

### 1.1 회원가입

```
POST /api/auth/join
권한: 전체 (비회원)
```

**Request Body**
```json
{
  "username": "hong1234",
  "password": "Pass1234!",
  "nickname": "홍길동"
}
```

**Response `201`**
```json
{
  "success": true,
  "message": "회원가입이 완료되었습니다.",
  "data": null
}
```

**Error Cases**
| 상황 | HTTP | ErrorCode |
|------|------|-----------|
| 아이디 중복 | 409 | DUPLICATE_USERNAME |
| 닉네임 중복 | 409 | DUPLICATE_NICKNAME |
| 유효성 오류 | 400 | INVALID_INPUT |

---

### 1.2 로그인

```
POST /api/auth/login
권한: 전체 (비회원)
```

**Request Body**
```json
{
  "username": "hong1234",
  "password": "Pass1234!"
}
```

**Response `200`**
```json
{
  "success": true,
  "message": "로그인되었습니다.",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "user": {
      "id": 1,
      "username": "hong1234",
      "nickname": "홍길동",
      "role": "USER"
    }
  }
}
```

Set-Cookie: `refresh={uuid}; HttpOnly; Secure; SameSite=Strict; Path=/api/auth; Max-Age=604800`

**Error Cases**
| 상황 | HTTP | ErrorCode |
|------|------|-----------|
| 아이디/비밀번호 불일치 | 401 | LOGIN_FAILED |
| 비활성 계정 | 403 | ACCOUNT_DISABLED |

---

### 1.3 토큰 갱신

```
POST /api/auth/refresh
권한: 전체 (Cookie의 Refresh Token 필요)
```

**Request**: Cookie `refresh={refreshToken}` 자동 포함 (별도 Body 없음)

**Response `200`**
```json
{
  "success": true,
  "message": "토큰이 갱신되었습니다.",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

**Error Cases**
| 상황 | HTTP | ErrorCode |
|------|------|-----------|
| Refresh Token 만료 | 401 | REFRESH_TOKEN_EXPIRED |
| Refresh Token 무효 | 401 | INVALID_REFRESH_TOKEN |

---

### 1.4 로그아웃

```
POST /api/auth/logout
권한: USER, ADMIN
```

**Request**: Body 없음 (Authorization 헤더 + Cookie)

**Response `200`**
```json
{
  "success": true,
  "message": "로그아웃되었습니다.",
  "data": null
}
```

Set-Cookie: `refresh=; HttpOnly; Max-Age=0` (쿠키 삭제)

---

## 2. 사용자 API

### 2.1 내 정보 조회

```
GET /api/users/me
권한: USER, ADMIN
```

**Response `200`**
```json
{
  "success": true,
  "message": "조회되었습니다.",
  "data": {
    "id": 1,
    "username": "hong1234",
    "nickname": "홍길동",
    "role": "USER",
    "aiUsageToday": 5,
    "aiUsageLimit": 20
  }
}
```

---

### 2.2 사용자 검색 (DM 상대 찾기)

```
GET /api/users/search?nickname={keyword}
권한: USER, ADMIN
```

**Query Parameters**
| 파라미터 | 필수 | 설명 |
|----------|------|------|
| nickname | ✅ | 검색 키워드 (최소 1자) |

**Response `200`**
```json
{
  "success": true,
  "message": "조회되었습니다.",
  "data": [
    {
      "id": 2,
      "nickname": "김철수",
      "isOnline": true
    },
    {
      "id": 3,
      "nickname": "김영희",
      "isOnline": false
    }
  ]
}
```

> 본인은 검색 결과에서 제외한다.

---

### 2.3 AI 사용량 조회

```
GET /api/users/me/ai-usage
권한: USER, ADMIN
```

**Response `200`**
```json
{
  "success": true,
  "message": "조회되었습니다.",
  "data": {
    "usedCount": 15,
    "limitCount": 20,
    "remainCount": 5,
    "resetAt": "2025-01-16T00:00:00"
  }
}
```

---

## 3. 채널 API

### 3.1 참여 채널 목록 조회

```
GET /api/channels
권한: USER, ADMIN
```

**Response `200`**
```json
{
  "success": true,
  "message": "조회되었습니다.",
  "data": [
    {
      "id": 1,
      "name": "일반",
      "description": "모두를 위한 일반 채팅 채널입니다.",
      "isPrivate": false,
      "unreadCount": 3,
      "lastMessage": {
        "content": "안녕하세요!",
        "createdAt": "2025-01-15T14:30:00"
      }
    }
  ]
}
```

---

### 3.2 채널 상세 조회

```
GET /api/channels/{channelId}
권한: USER, ADMIN
```

**Response `200`**
```json
{
  "success": true,
  "message": "조회되었습니다.",
  "data": {
    "id": 1,
    "name": "일반",
    "description": "모두를 위한 일반 채팅 채널입니다.",
    "isPrivate": false,
    "participantCount": 42,
    "noticeMessage": {
      "id": 100,
      "content": "채널 이용 규칙을 확인해 주세요.",
      "createdAt": "2025-01-10T09:00:00"
    }
  }
}
```

**Error Cases**
| 상황 | HTTP | ErrorCode |
|------|------|-----------|
| 채널 없음 | 404 | CHANNEL_NOT_FOUND |

---

### 3.3 채널 메시지 목록 조회 (커서 기반)

```
GET /api/channels/{channelId}/messages?size=20&before={messageId}
권한: USER, ADMIN
```

**Query Parameters**
| 파라미터 | 필수 | 기본값 | 설명 |
|----------|------|--------|------|
| size | ❌ | 20 | 조회할 메시지 수 (최대 50) |
| before | ❌ | 없음 | 이 ID보다 이전 메시지 조회 (무한 스크롤용) |

**Response `200`**
```json
{
  "success": true,
  "message": "조회되었습니다.",
  "data": {
    "content": [
      {
        "id": 105,
        "userId": 2,
        "nickname": "김철수",
        "content": "안녕하세요!",
        "type": "TEXT",
        "isDeleted": false,
        "isAiMessage": false,
        "isOwn": false,
        "attachments": [],
        "reactions": [
          { "emoji": "👍", "count": 3, "isMyReaction": true }
        ],
        "createdAt": "2025-01-15T14:30:00",
        "updatedAt": "2025-01-15T14:30:00"
      },
      {
        "id": 106,
        "userId": null,
        "nickname": "ChatterAI",
        "content": "안녕하세요! 무엇을 도와드릴까요?",
        "type": "AI_RESPONSE",
        "isDeleted": false,
        "isAiMessage": true,
        "isOwn": false,
        "attachments": [],
        "reactions": [],
        "createdAt": "2025-01-15T14:30:05",
        "updatedAt": "2025-01-15T14:30:05"
      }
    ],
    "hasNext": true,
    "nextCursorId": 104,
    "size": 20
  }
}
```

> `isOwn`: 요청한 사용자가 작성한 메시지 여부 (수정/삭제 버튼 노출 조건)  
> `isDeleted = true`인 메시지: content를 `"삭제된 메시지입니다."`로 반환, attachments·reactions 빈 배열

---

### 3.4 채널 읽음 처리

```
DELETE /api/channels/{channelId}/unread
권한: USER, ADMIN
```

**Response `200`**
```json
{
  "success": true,
  "message": "읽음 처리되었습니다.",
  "data": null
}
```

---

## 4. 메시지 API

> 메시지 **전송**은 REST가 아닌 **WebSocket STOMP**로 처리한다. (섹션 9 참조)  
> 아래 REST API는 수정·삭제 전용이다.

### 4.1 메시지 수정

```
PUT /api/messages/{messageId}
권한: USER (본인), ADMIN
```

**Request Body**
```json
{
  "content": "수정된 내용입니다."
}
```

**Response `200`**
```json
{
  "success": true,
  "message": "수정되었습니다.",
  "data": {
    "id": 105,
    "content": "수정된 내용입니다.",
    "updatedAt": "2025-01-15T15:00:00"
  }
}
```

**Error Cases**
| 상황 | HTTP | ErrorCode |
|------|------|-----------|
| 메시지 없음 | 404 | MESSAGE_NOT_FOUND |
| 타인 메시지 수정 시도 | 403 | FORBIDDEN |
| AI 메시지 수정 시도 | 403 | AI_MESSAGE_NOT_EDITABLE |
| 삭제된 메시지 | 400 | DELETED_MESSAGE |

---

### 4.2 메시지 삭제

```
DELETE /api/messages/{messageId}
권한: USER (본인), ADMIN
```

**Response `200`**
```json
{
  "success": true,
  "message": "삭제되었습니다.",
  "data": null
}
```

**Error Cases**
| 상황 | HTTP | ErrorCode |
|------|------|-----------|
| 메시지 없음 | 404 | MESSAGE_NOT_FOUND |
| 타인 메시지 삭제 시도 | 403 | FORBIDDEN |
| 이미 삭제된 메시지 | 400 | DELETED_MESSAGE |

---

## 5. DM API

### 5.1 내 DM 방 목록 조회

```
GET /api/dm/rooms
권한: USER, ADMIN
```

**Response `200`**
```json
{
  "success": true,
  "message": "조회되었습니다.",
  "data": [
    {
      "id": 1,
      "opponent": {
        "id": 3,
        "nickname": "김영희",
        "isOnline": true
      },
      "unreadCount": 2,
      "lastMessage": {
        "content": "내일 회의 몇 시예요?",
        "createdAt": "2025-01-15T13:00:00"
      }
    }
  ]
}
```

---

### 5.2 DM 방 생성 또는 기존 방 조회

```
POST /api/dm/rooms
권한: USER, ADMIN
```

**Request Body**
```json
{
  "targetUserId": 3
}
```

**Response `201` (신규 생성)**
```json
{
  "success": true,
  "message": "DM 방이 생성되었습니다.",
  "data": {
    "id": 5,
    "opponent": {
      "id": 3,
      "nickname": "김영희",
      "isOnline": true
    }
  }
}
```

**Response `200` (기존 방)**
```json
{
  "success": true,
  "message": "기존 DM 방으로 이동합니다.",
  "data": {
    "id": 1,
    "opponent": {
      "id": 3,
      "nickname": "김영희",
      "isOnline": true
    }
  }
}
```

**Error Cases**
| 상황 | HTTP | ErrorCode |
|------|------|-----------|
| 상대방 없음 | 404 | USER_NOT_FOUND |
| 본인과 DM 시도 | 400 | SELF_DM_NOT_ALLOWED |

---

### 5.3 DM 메시지 목록 조회 (커서 기반)

```
GET /api/dm/rooms/{dmRoomId}/messages?size=20&before={messageId}
권한: USER (참여자), ADMIN
```

**Response `200`**: 채널 메시지 조회(3.3)와 동일한 구조

**Error Cases**
| 상황 | HTTP | ErrorCode |
|------|------|-----------|
| DM 방 없음 | 404 | DM_ROOM_NOT_FOUND |
| 참여하지 않은 DM 방 | 403 | FORBIDDEN |

---

### 5.4 DM 읽음 처리

```
DELETE /api/dm/rooms/{dmRoomId}/unread
권한: USER (참여자), ADMIN
```

**Response `200`**
```json
{
  "success": true,
  "message": "읽음 처리되었습니다.",
  "data": null
}
```

---

## 6. 파일 API

### 6.1 파일 업로드

```
POST /api/files/upload
권한: USER, ADMIN
Content-Type: multipart/form-data
```

**Request (Form Data)**
| 필드 | 타입 | 설명 |
|------|------|------|
| files | File[] | 업로드할 파일 (최대 5개) |

**Response `200`**
```json
{
  "success": true,
  "message": "업로드되었습니다.",
  "data": [
    {
      "fileUrl": "/api/files/uploads/chat/2025/01/uuid-filename.jpg",
      "fileName": "filename.jpg",
      "fileSize": 204800,
      "fileType": "IMAGE"
    }
  ]
}
```

**Error Cases**
| 상황 | HTTP | ErrorCode |
|------|------|-----------|
| 파일 개수 초과 | 400 | FILE_COUNT_EXCEEDED |
| 파일 크기 초과 | 400 | FILE_SIZE_EXCEEDED |
| 허용되지 않는 확장자 | 400 | FILE_EXTENSION_INVALID |

---

### 6.2 파일 서빙

```
GET /api/files/uploads/**
권한: USER, ADMIN
```

Spring 정적 리소스 핸들러로 로컬 파일 시스템의 파일을 직접 서빙한다.

---

## 7. 이모지 반응 API

### 7.1 이모지 반응 추가

```
POST /api/messages/{messageId}/reactions
권한: USER, ADMIN
```

**Request Body**
```json
{
  "emoji": "👍"
}
```

**Response `201`**
```json
{
  "success": true,
  "message": "반응이 추가되었습니다.",
  "data": {
    "messageId": 105,
    "emoji": "👍",
    "count": 4,
    "isMyReaction": true
  }
}
```

**Error Cases**
| 상황 | HTTP | ErrorCode |
|------|------|-----------|
| 이미 반응한 이모지 | 409 | REACTION_ALREADY_EXISTS |
| 메시지 없음 | 404 | MESSAGE_NOT_FOUND |
| 삭제된 메시지 | 400 | DELETED_MESSAGE |

---

### 7.2 이모지 반응 취소

```
DELETE /api/messages/{messageId}/reactions
권한: USER, ADMIN
```

**Request Body**
```json
{
  "emoji": "👍"
}
```

**Response `200`**
```json
{
  "success": true,
  "message": "반응이 취소되었습니다.",
  "data": {
    "messageId": 105,
    "emoji": "👍",
    "count": 3,
    "isMyReaction": false
  }
}
```

**Error Cases**
| 상황 | HTTP | ErrorCode |
|------|------|-----------|
| 반응하지 않은 이모지 취소 시도 | 404 | REACTION_NOT_FOUND |

---

## 8. 관리자 API

> 모든 관리자 API는 `ADMIN` 권한 필수. 일반 회원 접근 시 `403 FORBIDDEN`.

### 8.1 채널 생성

```
POST /api/admin/channels
권한: ADMIN
```

**Request Body**
```json
{
  "name": "개발팀",
  "description": "개발팀 전용 채널",
  "isPrivate": false
}
```

**Response `201`**
```json
{
  "success": true,
  "message": "채널이 생성되었습니다.",
  "data": {
    "id": 4,
    "name": "개발팀",
    "description": "개발팀 전용 채널",
    "isPrivate": false,
    "createdAt": "2025-01-15T10:00:00"
  }
}
```

**Error Cases**
| 상황 | HTTP | ErrorCode |
|------|------|-----------|
| 채널명 중복 | 409 | DUPLICATE_CHANNEL_NAME |
| 유효성 오류 | 400 | INVALID_INPUT |

---

### 8.2 채널 삭제

```
DELETE /api/admin/channels/{channelId}
권한: ADMIN
```

**Response `200`**
```json
{
  "success": true,
  "message": "채널이 삭제되었습니다.",
  "data": null
}
```

**Error Cases**
| 상황 | HTTP | ErrorCode |
|------|------|-----------|
| 채널 없음 | 404 | CHANNEL_NOT_FOUND |

---

### 8.3 공지 메시지 지정

```
POST /api/admin/channels/{channelId}/notice
권한: ADMIN
```

**Request Body**
```json
{
  "messageId": 100
}
```

**Response `200`**
```json
{
  "success": true,
  "message": "공지가 설정되었습니다.",
  "data": {
    "channelId": 1,
    "noticeMessage": {
      "id": 100,
      "content": "채널 이용 규칙을 확인해 주세요.",
      "createdAt": "2025-01-10T09:00:00"
    }
  }
}
```

---

### 8.4 공지 메시지 해제

```
DELETE /api/admin/channels/{channelId}/notice
권한: ADMIN
```

**Response `200`**
```json
{
  "success": true,
  "message": "공지가 해제되었습니다.",
  "data": null
}
```

---

### 8.5 회원 목록 조회

```
GET /api/admin/users?page=1&size=20&keyword={nickname}
권한: ADMIN
```

**Query Parameters**
| 파라미터 | 필수 | 기본값 | 설명 |
|----------|------|--------|------|
| page | ❌ | 1 | 페이지 번호 |
| size | ❌ | 20 | 페이지 크기 |
| keyword | ❌ | — | 닉네임 검색 |

**Response `200`**
```json
{
  "success": true,
  "message": "조회되었습니다.",
  "data": {
    "content": [
      {
        "id": 2,
        "username": "hong1234",
        "nickname": "홍길동",
        "role": "USER",
        "isActive": true,
        "isOnline": false,
        "createdAt": "2025-01-01T00:00:00"
      }
    ],
    "totalCount": 87,
    "totalPages": 5,
    "currentPage": 1,
    "pageSize": 20
  }
}
```

---

### 8.6 회원 비활성화

```
PUT /api/admin/users/{userId}/disable
권한: ADMIN
```

**Response `200`**
```json
{
  "success": true,
  "message": "계정이 비활성화되었습니다.",
  "data": null
}
```

**Error Cases**
| 상황 | HTTP | ErrorCode |
|------|------|-----------|
| 회원 없음 | 404 | USER_NOT_FOUND |
| 본인 비활성화 시도 | 400 | SELF_DISABLE_NOT_ALLOWED |

---

### 8.7 회원 활성화

```
PUT /api/admin/users/{userId}/enable
권한: ADMIN
```

**Response `200`**
```json
{
  "success": true,
  "message": "계정이 활성화되었습니다.",
  "data": null
}
```

---

## 9. WebSocket STOMP 명세

### 9.1 연결 엔드포인트

```
개발: ws://localhost:8080/ws
운영: wss://{도메인}/ws

SockJS 폴백: /ws (SockJS 클라이언트 사용 시 자동 처리)
```

### 9.2 CONNECT 프레임

```
CONNECT
Authorization: Bearer {accessToken}
accept-version: 1.1,1.0
heart-beat: 10000,10000
```

---

### 9.3 구독 경로 (Subscribe)

| 경로 | 설명 | 수신 메시지 타입 |
|------|------|-----------------|
| `/topic/channel/{channelId}` | 채널 메시지 수신 | CHAT, AI_LOADING, AI_RESPONSE, AI_ERROR, MESSAGE_UPDATED, MESSAGE_DELETED, NOTICE_UPDATED, CHANNEL_DELETED, REACTION_UPDATED |
| `/topic/channel/{channelId}/typing` | 타이핑 인디케이터 | TYPING |
| `/topic/dm/{dmRoomId}` | DM 메시지 수신 | CHAT, AI_LOADING, AI_RESPONSE, AI_ERROR, MESSAGE_UPDATED, MESSAGE_DELETED |
| `/user/queue/notification` | 개인 알림 | UNREAD_COUNT |

---

### 9.4 발행 경로 (Publish — Client → Server)

| 경로 | 설명 | Payload |
|------|------|---------|
| `/app/channel/{channelId}/send` | 채널 메시지 전송 | `ChatSendRequest` |
| `/app/dm/{dmRoomId}/send` | DM 메시지 전송 | `ChatSendRequest` |
| `/app/channel/{channelId}/typing` | 타이핑 상태 전송 | `{}` (빈 객체) |
| `/app/presence/heartbeat` | 온라인 하트비트 | `{}` (빈 객체) |
| `/app/presence/away` | 자리비움 전환 | `{}` (빈 객체) |

---

### 9.5 메시지 Payload 상세

#### ChatSendRequest (Client → Server)

```json
{
  "content": "안녕하세요! @AI 오늘 날씨 어때?",
  "type": "TEXT",
  "attachments": [
    {
      "fileUrl": "/api/files/uploads/chat/uuid-file.jpg",
      "fileName": "file.jpg",
      "fileSize": 204800,
      "fileType": "IMAGE"
    }
  ]
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| content | String | ✅ | 메시지 내용 |
| type | Enum | ✅ | `TEXT` 또는 `FILE` |
| attachments | Array | ❌ | 파일 첨부 시 포함 (사전에 `/api/files/upload` 호출) |

---

#### ChatMessageResponse (Server → Client 브로드캐스트)

```json
{
  "eventType": "CHAT",
  "message": {
    "id": 110,
    "channelId": 1,
    "dmRoomId": null,
    "userId": 2,
    "nickname": "홍길동",
    "content": "안녕하세요!",
    "type": "TEXT",
    "isDeleted": false,
    "isAiMessage": false,
    "attachments": [],
    "reactions": [],
    "createdAt": "2025-01-15T14:30:00",
    "updatedAt": "2025-01-15T14:30:00"
  }
}
```

---

#### AI 로딩 메시지 (Server → Client 브로드캐스트)

```json
{
  "eventType": "AI_LOADING",
  "tempId": "ai-temp-uuid-1234",
  "channelId": 1,
  "content": "AI가 답변을 생성 중입니다..."
}
```

---

#### AI 응답 완료 (Server → Client 브로드캐스트)

```json
{
  "eventType": "AI_RESPONSE",
  "tempId": "ai-temp-uuid-1234",
  "message": {
    "id": 111,
    "channelId": 1,
    "userId": null,
    "nickname": "ChatterAI",
    "content": "오늘 서울 날씨는 맑고 기온은 15도입니다.",
    "type": "AI_RESPONSE",
    "isDeleted": false,
    "isAiMessage": true,
    "attachments": [],
    "reactions": [],
    "createdAt": "2025-01-15T14:30:05",
    "updatedAt": "2025-01-15T14:30:05"
  }
}
```

---

#### 메시지 수정 이벤트 (Server → Client 브로드캐스트)

```json
{
  "eventType": "MESSAGE_UPDATED",
  "message": {
    "id": 110,
    "content": "수정된 내용입니다.",
    "updatedAt": "2025-01-15T15:00:00"
  }
}
```

---

#### 메시지 삭제 이벤트 (Server → Client 브로드캐스트)

```json
{
  "eventType": "MESSAGE_DELETED",
  "messageId": 110
}
```

---

#### 타이핑 이벤트 (Server → Client 브로드캐스트)

```json
{
  "eventType": "TYPING",
  "userId": 2,
  "nickname": "홍길동"
}
```

---

#### 이모지 반응 업데이트 (Server → Client 브로드캐스트)

```json
{
  "eventType": "REACTION_UPDATED",
  "messageId": 110,
  "reactions": [
    { "emoji": "👍", "count": 4 },
    { "emoji": "😂", "count": 1 }
  ]
}
```

---

#### 채널 삭제 이벤트 (Server → Client 브로드캐스트)

```json
{
  "eventType": "CHANNEL_DELETED",
  "channelId": 3
}
```

---

#### 안읽은 수 알림 (Server → 개인 알림 `/user/queue/notification`)

```json
{
  "eventType": "UNREAD_COUNT",
  "channelId": 2,
  "unreadCount": 5
}
```

---

## 10. API 엔드포인트 요약

| 도메인 | Method | URI | 권한 |
|--------|--------|-----|------|
| 인증 | POST | `/api/auth/join` | 전체 |
| 인증 | POST | `/api/auth/login` | 전체 |
| 인증 | POST | `/api/auth/refresh` | 전체 (Cookie) |
| 인증 | POST | `/api/auth/logout` | USER, ADMIN |
| 사용자 | GET | `/api/users/me` | USER, ADMIN |
| 사용자 | GET | `/api/users/search` | USER, ADMIN |
| 사용자 | GET | `/api/users/me/ai-usage` | USER, ADMIN |
| 채널 | GET | `/api/channels` | USER, ADMIN |
| 채널 | GET | `/api/channels/{id}` | USER, ADMIN |
| 채널 | GET | `/api/channels/{id}/messages` | USER, ADMIN |
| 채널 | DELETE | `/api/channels/{id}/unread` | USER, ADMIN |
| 메시지 | PUT | `/api/messages/{id}` | USER(본인), ADMIN |
| 메시지 | DELETE | `/api/messages/{id}` | USER(본인), ADMIN |
| DM | GET | `/api/dm/rooms` | USER, ADMIN |
| DM | POST | `/api/dm/rooms` | USER, ADMIN |
| DM | GET | `/api/dm/rooms/{id}/messages` | USER(참여자), ADMIN |
| DM | DELETE | `/api/dm/rooms/{id}/unread` | USER(참여자), ADMIN |
| 파일 | POST | `/api/files/upload` | USER, ADMIN |
| 파일 | GET | `/api/files/uploads/**` | USER, ADMIN |
| 반응 | POST | `/api/messages/{id}/reactions` | USER, ADMIN |
| 반응 | DELETE | `/api/messages/{id}/reactions` | USER, ADMIN |
| 관리자 | POST | `/api/admin/channels` | ADMIN |
| 관리자 | DELETE | `/api/admin/channels/{id}` | ADMIN |
| 관리자 | POST | `/api/admin/channels/{id}/notice` | ADMIN |
| 관리자 | DELETE | `/api/admin/channels/{id}/notice` | ADMIN |
| 관리자 | GET | `/api/admin/users` | ADMIN |
| 관리자 | PUT | `/api/admin/users/{id}/disable` | ADMIN |
| 관리자 | PUT | `/api/admin/users/{id}/enable` | ADMIN |
