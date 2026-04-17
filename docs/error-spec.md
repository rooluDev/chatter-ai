# ❌ Error Spec

## ChatterAI — 실시간 채팅 + AI 어시스턴트 플랫폼

> 이 문서는 전체 에러 코드 목록, HTTP 상태 코드, 프론트엔드 처리 방식을 정의한다.  
> 구현 시 백엔드 `ErrorCode` enum과 프론트엔드 Axios 인터셉터는 이 문서를 기준으로 작성한다.

---

## 목차

1. [공통 에러 처리 원칙](#1-공통-에러-처리-원칙)
2. [백엔드 에러 처리 구조](#2-백엔드-에러-처리-구조)
3. [프론트엔드 에러 처리 구조](#3-프론트엔드-에러-처리-구조)
4. [에러 코드 전체 목록](#4-에러-코드-전체-목록)
5. [WebSocket 에러 처리](#5-websocket-에러-처리)
6. [AI 멘션 에러 처리](#6-ai-멘션-에러-처리)

---

## 1. 공통 에러 처리 원칙

- **백엔드**: 모든 에러는 `CustomException(ErrorCode)` throw → `GlobalExceptionHandler`가 공통 응답 형식으로 변환
- **프론트엔드**: Axios 인터셉터에서 HTTP 상태 코드별 공통 처리 후, 개별 케이스는 컴포넌트에서 추가 처리
- **서버 내부 정보 노출 금지**: 스택 트레이스, SQL 쿼리, 내부 클래스명은 절대 응답에 포함하지 않는다
- **에러 응답 형식 통일**: 모든 에러는 아래 형식을 따른다

```json
{
  "success": false,
  "message": "사용자에게 보여줄 에러 메시지",
  "data": null
}
```

---

## 2. 백엔드 에러 처리 구조

### 2.1 ErrorCode enum 구조

```java
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 인증
    INVALID_INPUT(400, "입력값이 올바르지 않습니다."),
    LOGIN_FAILED(401, "아이디 또는 비밀번호가 올바르지 않습니다."),
    UNAUTHORIZED(401, "로그인이 필요합니다."),
    REFRESH_TOKEN_EXPIRED(401, "세션이 만료되었습니다. 다시 로그인해 주세요."),
    INVALID_REFRESH_TOKEN(401, "유효하지 않은 인증 정보입니다."),
    FORBIDDEN(403, "접근 권한이 없습니다."),
    ACCOUNT_DISABLED(403, "비활성화된 계정입니다. 관리자에게 문의해 주세요."),

    // 사용자
    USER_NOT_FOUND(404, "사용자를 찾을 수 없습니다."),
    DUPLICATE_USERNAME(409, "이미 사용 중인 아이디입니다."),
    DUPLICATE_NICKNAME(409, "이미 사용 중인 닉네임입니다."),
    SELF_DM_NOT_ALLOWED(400, "본인에게 DM을 보낼 수 없습니다."),
    SELF_DISABLE_NOT_ALLOWED(400, "본인 계정은 비활성화할 수 없습니다."),

    // 채널
    CHANNEL_NOT_FOUND(404, "채널을 찾을 수 없습니다."),
    DUPLICATE_CHANNEL_NAME(409, "이미 사용 중인 채널 이름입니다."),
    CHANNEL_ACCESS_DENIED(403, "채널에 접근할 권한이 없습니다."),

    // 메시지
    MESSAGE_NOT_FOUND(404, "메시지를 찾을 수 없습니다."),
    DELETED_MESSAGE(400, "이미 삭제된 메시지입니다."),
    AI_MESSAGE_NOT_EDITABLE(403, "AI 응답 메시지는 수정할 수 없습니다."),

    // DM
    DM_ROOM_NOT_FOUND(404, "DM 방을 찾을 수 없습니다."),
    DM_ACCESS_DENIED(403, "DM 방에 접근할 권한이 없습니다."),

    // 파일
    FILE_COUNT_EXCEEDED(400, "파일은 최대 5개까지 첨부할 수 있습니다."),
    FILE_SIZE_EXCEEDED(400, "파일 크기는 허용 용량을 초과할 수 없습니다."),
    FILE_EXTENSION_INVALID(400, "허용되지 않는 파일 형식입니다."),
    FILE_UPLOAD_FAILED(500, "파일 업로드 중 오류가 발생했습니다."),

    // 이모지 반응
    REACTION_ALREADY_EXISTS(409, "이미 반응한 이모지입니다."),
    REACTION_NOT_FOUND(404, "반응을 찾을 수 없습니다."),

    // AI
    AI_USAGE_EXCEEDED(429, "오늘의 AI 사용 횟수(20회)를 모두 사용했습니다."),
    AI_API_ERROR(502, "AI 응답 생성에 실패했습니다. 잠시 후 다시 시도해 주세요."),
    AI_API_TIMEOUT(504, "AI 응답 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요."),

    // 공통
    SERVER_ERROR(500, "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");

    private final int status;
    private final String message;
}
```

### 2.2 CustomException

```java
@Getter
public class CustomException extends RuntimeException {
    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
```

### 2.3 GlobalExceptionHandler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 비즈니스 에러 (CustomException)
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        ErrorCode code = e.getErrorCode();
        return ResponseEntity
            .status(code.getStatus())
            .body(ApiResponse.error(code.getMessage()));
    }

    // @Valid 유효성 검사 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidException(
            MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .findFirst()
            .orElse(ErrorCode.INVALID_INPUT.getMessage());
        return ResponseEntity
            .status(400)
            .body(ApiResponse.error(message));
    }

    // JWT 인증 실패 (Spring Security)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity
            .status(403)
            .body(ApiResponse.error(ErrorCode.FORBIDDEN.getMessage()));
    }

    // 예상치 못한 서버 에러
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unexpected error: ", e);  // 서버 로그에만 기록
        return ResponseEntity
            .status(500)
            .body(ApiResponse.error(ErrorCode.SERVER_ERROR.getMessage()));
    }
}
```

---

## 3. 프론트엔드 에러 처리 구조

### 3.1 HTTP 상태 코드별 공통 처리 (Axios 인터셉터)

```javascript
// api/axios.js

axios.interceptors.response.use(
  (response) => response,
  async (error) => {
    const status = error.response?.status;
    const originalRequest = error.config;

    // 401 — 인증 만료
    if (status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      try {
        // Refresh Token으로 Access Token 갱신
        const { data } = await axios.post('/api/auth/refresh');
        const newToken = data.data.accessToken;
        authStore.setAccessToken(newToken);
        originalRequest.headers['Authorization'] = `Bearer ${newToken}`;
        return axios(originalRequest);  // 원래 요청 재시도
      } catch (refreshError) {
        // Refresh Token도 만료 → 로그아웃
        authStore.clear();
        const returnPath = router.currentRoute.value.fullPath;
        router.push(`/login?ret=${encodeURIComponent(returnPath)}`);
        return Promise.reject(refreshError);
      }
    }

    // 403 — 권한 없음 → 토스트
    if (status === 403) {
      const message = error.response?.data?.message || '접근 권한이 없습니다.';
      uiStore.showToast('error', message);
    }

    // 404 — 리소스 없음 → 토스트 (컴포넌트에서 추가 처리 가능)
    if (status === 404) {
      const message = error.response?.data?.message || '요청한 리소스를 찾을 수 없습니다.';
      uiStore.showToast('error', message);
    }

    // 429 — 사용량 초과 → 토스트
    if (status === 429) {
      const message = error.response?.data?.message || '요청 한도를 초과했습니다.';
      uiStore.showToast('error', message);
    }

    // 500 / 502 / 504 — 서버 에러 → 토스트
    if (status >= 500) {
      const message = error.response?.data?.message || '서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.';
      uiStore.showToast('error', message);
    }

    return Promise.reject(error);
  }
);
```

### 3.2 HTTP 상태 코드별 처리 방식 요약

| HTTP 상태 | 공통 처리 | 컴포넌트 추가 처리 |
|-----------|-----------|-------------------|
| 400 | — | 해당 필드 하단 에러 메시지 표시 |
| 401 (첫 번째) | Refresh Token으로 재발급 후 재시도 | — |
| 401 (재시도 후도 실패) | 로그아웃 + `/login?ret=...` 이동 | — |
| 403 | 토스트 에러 표시 | — |
| 404 | 토스트 에러 표시 | 목록 페이지로 이동 (선택적) |
| 409 | — | 해당 필드 하단 에러 메시지 표시 |
| 429 | 토스트 에러 표시 | — |
| 500 / 502 / 504 | 토스트 에러 표시 | — |
| 네트워크 오류 | 토스트 "네트워크 연결을 확인해 주세요." | — |

### 3.3 컴포넌트별 개별 에러 처리

**로그인 폼**
```javascript
try {
  await authApi.login({ username, password });
} catch (error) {
  if (error.response?.status === 401) {
    // 인터셉터가 토큰 갱신 시도 안 함 (로그인 요청은 _retry 없음)
    formError.value = error.response.data.message;
    // "아이디 또는 비밀번호가 올바르지 않습니다."
  }
  if (error.response?.status === 403) {
    formError.value = error.response.data.message;
    // "비활성화된 계정입니다. 관리자에게 문의해 주세요."
  }
}
```

**회원가입 폼**
```javascript
try {
  await authApi.join({ username, password, nickname });
} catch (error) {
  if (error.response?.status === 409) {
    const code = error.response.data.errorCode;
    if (code === 'DUPLICATE_USERNAME') {
      usernameError.value = '이미 사용 중인 아이디입니다.';
    }
    if (code === 'DUPLICATE_NICKNAME') {
      nicknameError.value = '이미 사용 중인 닉네임입니다.';
    }
  }
}
```

> **주의**: 409 에러는 공통 인터셉터에서 토스트를 띄우지 않는다.  
> 회원가입·채널 생성 등 폼에서 필드별 에러 표시가 필요하기 때문에 컴포넌트에서 직접 처리한다.

**메시지 수정/삭제**
```javascript
try {
  await messageApi.delete(messageId);
  // Optimistic UI: 성공 시 로컬 상태에서 제거
} catch (error) {
  // 404: 이미 삭제된 메시지 → 토스트 (인터셉터 처리)
  // 403: 권한 없음 → 토스트 (인터셉터 처리)
  // Optimistic UI 롤백: 로컬 상태 원복
  messageStore.rollbackDelete(messageId);
}
```

---

## 4. 에러 코드 전체 목록

### 4.1 인증 (Auth)

| ErrorCode | HTTP | 메시지 | 발생 상황 | 프론트 처리 |
|-----------|------|--------|-----------|-------------|
| `INVALID_INPUT` | 400 | 입력값이 올바르지 않습니다. | @Valid 실패, 필수값 누락 | 폼 필드 하단 에러 표시 |
| `LOGIN_FAILED` | 401 | 아이디 또는 비밀번호가 올바르지 않습니다. | 로그인 실패 | 폼 하단 에러 표시 |
| `UNAUTHORIZED` | 401 | 로그인이 필요합니다. | JWT 없음 또는 만료 | 토큰 갱신 시도 → 실패 시 /login |
| `REFRESH_TOKEN_EXPIRED` | 401 | 세션이 만료되었습니다. 다시 로그인해 주세요. | Refresh Token 만료 | /login?ret=... 이동 |
| `INVALID_REFRESH_TOKEN` | 401 | 유효하지 않은 인증 정보입니다. | Refresh Token 위조·없음 | /login?ret=... 이동 |
| `FORBIDDEN` | 403 | 접근 권한이 없습니다. | 권한 없는 리소스 접근 | 토스트 에러 |
| `ACCOUNT_DISABLED` | 403 | 비활성화된 계정입니다. 관리자에게 문의해 주세요. | 비활성 계정 로그인 | 폼 하단 에러 표시 |

### 4.2 사용자 (User)

| ErrorCode | HTTP | 메시지 | 발생 상황 | 프론트 처리 |
|-----------|------|--------|-----------|-------------|
| `USER_NOT_FOUND` | 404 | 사용자를 찾을 수 없습니다. | 존재하지 않는 사용자 조회 | 토스트 에러 |
| `DUPLICATE_USERNAME` | 409 | 이미 사용 중인 아이디입니다. | 아이디 중복 | 아이디 필드 하단 에러 |
| `DUPLICATE_NICKNAME` | 409 | 이미 사용 중인 닉네임입니다. | 닉네임 중복 | 닉네임 필드 하단 에러 |
| `SELF_DM_NOT_ALLOWED` | 400 | 본인에게 DM을 보낼 수 없습니다. | 본인 DM 시도 | 토스트 에러 |
| `SELF_DISABLE_NOT_ALLOWED` | 400 | 본인 계정은 비활성화할 수 없습니다. | 관리자 본인 비활성화 | 토스트 에러 |

### 4.3 채널 (Channel)

| ErrorCode | HTTP | 메시지 | 발생 상황 | 프론트 처리 |
|-----------|------|--------|-----------|-------------|
| `CHANNEL_NOT_FOUND` | 404 | 채널을 찾을 수 없습니다. | 존재하지 않는 채널 | 토스트 + 첫 채널로 이동 |
| `DUPLICATE_CHANNEL_NAME` | 409 | 이미 사용 중인 채널 이름입니다. | 채널명 중복 | 채널명 필드 하단 에러 |
| `CHANNEL_ACCESS_DENIED` | 403 | 채널에 접근할 권한이 없습니다. | 비공개 채널 미초대 접근 | 토스트 에러 |

### 4.4 메시지 (Message)

| ErrorCode | HTTP | 메시지 | 발생 상황 | 프론트 처리 |
|-----------|------|--------|-----------|-------------|
| `MESSAGE_NOT_FOUND` | 404 | 메시지를 찾을 수 없습니다. | 존재하지 않는 메시지 | 토스트 에러 + Optimistic UI 롤백 |
| `DELETED_MESSAGE` | 400 | 이미 삭제된 메시지입니다. | 삭제된 메시지 수정·반응 시도 | 토스트 에러 |
| `AI_MESSAGE_NOT_EDITABLE` | 403 | AI 응답 메시지는 수정할 수 없습니다. | AI 메시지 수정 시도 | 토스트 에러 (정상 경로에서는 수정 버튼 미노출이므로 비정상 접근) |

### 4.5 DM

| ErrorCode | HTTP | 메시지 | 발생 상황 | 프론트 처리 |
|-----------|------|--------|-----------|-------------|
| `DM_ROOM_NOT_FOUND` | 404 | DM 방을 찾을 수 없습니다. | 존재하지 않는 DM 방 | 토스트 + DM 목록으로 이동 |
| `DM_ACCESS_DENIED` | 403 | DM 방에 접근할 권한이 없습니다. | 타인의 DM 방 접근 | 토스트 에러 |

### 4.6 파일 (File)

| ErrorCode | HTTP | 메시지 | 발생 상황 | 프론트 처리 |
|-----------|------|--------|-----------|-------------|
| `FILE_COUNT_EXCEEDED` | 400 | 파일은 최대 5개까지 첨부할 수 있습니다. | 5개 초과 첨부 | 토스트 에러 (프론트 1차 차단 후 서버 2차 검증) |
| `FILE_SIZE_EXCEEDED` | 400 | 파일 크기는 허용 용량을 초과할 수 없습니다. | 용량 초과 파일 | 토스트 에러 (프론트 1차 차단 후 서버 2차 검증) |
| `FILE_EXTENSION_INVALID` | 400 | 허용되지 않는 파일 형식입니다. | 이미지 외 파일 첨부 시도 | 토스트 에러 |
| `FILE_UPLOAD_FAILED` | 500 | 파일 업로드 중 오류가 발생했습니다. | 서버 IO 오류 | 토스트 에러 |

### 4.7 이모지 반응 (Reaction)

| ErrorCode | HTTP | 메시지 | 발생 상황 | 프론트 처리 |
|-----------|------|--------|-----------|-------------|
| `REACTION_ALREADY_EXISTS` | 409 | 이미 반응한 이모지입니다. | 동일 이모지 중복 반응 | 자동으로 반응 취소 API 호출로 전환 |
| `REACTION_NOT_FOUND` | 404 | 반응을 찾을 수 없습니다. | 없는 반응 취소 시도 | 토스트 에러 (정상 경로에서는 발생 안 함) |

### 4.8 AI

| ErrorCode | HTTP | 메시지 | 발생 상황 | 프론트 처리 |
|-----------|------|--------|-----------|-------------|
| `AI_USAGE_EXCEEDED` | 429 | 오늘의 AI 사용 횟수(20회)를 모두 사용했습니다. | 일 20회 초과 | 토스트 에러 (프론트 1차 차단 후 서버 2차 검증) |
| `AI_API_ERROR` | 502 | AI 응답 생성에 실패했습니다. 잠시 후 다시 시도해 주세요. | Claude API 에러 | AI 로딩 메시지 → 에러 메시지로 교체 (채널 브로드캐스트) |
| `AI_API_TIMEOUT` | 504 | AI 응답 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요. | 10초 타임아웃 | AI 로딩 메시지 → 에러 메시지로 교체 (채널 브로드캐스트) |

### 4.9 공통 (Common)

| ErrorCode | HTTP | 메시지 | 발생 상황 | 프론트 처리 |
|-----------|------|--------|-----------|-------------|
| `SERVER_ERROR` | 500 | 서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요. | 예상치 못한 예외 | 토스트 에러 |

---

## 5. WebSocket 에러 처리

### 5.1 STOMP 연결 에러

```javascript
// socket/stompClient.js

const client = new Client({
  // ...

  onStompError: (frame) => {
    const errorMessage = frame.headers?.message || '';

    if (errorMessage.includes('UNAUTHORIZED')) {
      // JWT 만료 → Refresh Token으로 갱신 후 재연결
      handleTokenRefreshAndReconnect();
    } else {
      // 기타 에러 → 토스트 표시
      uiStore.showToast('error', '채팅 연결에 문제가 발생했습니다.');
    }
  },

  onDisconnect: () => {
    // 의도하지 않은 연결 끊김 → 재연결 시도
    scheduleReconnect();
  },

  onWebSocketError: (event) => {
    uiStore.showToast('error', '네트워크 연결을 확인해 주세요.');
    scheduleReconnect();
  }
});
```

### 5.2 STOMP 재연결 전략

```
연결 끊김 감지
  └─ 1차 재연결 시도 (즉시)
       ├─ 성공 → 정상 복귀
       └─ 실패
            └─ 2차 재연결 시도 (3초 후)
                 ├─ 성공 → 정상 복귀
                 └─ 실패
                      └─ 3차 재연결 시도 (5초 후)
                           ├─ 성공 → 정상 복귀
                           └─ 실패 → 토스트 "연결이 끊어졌습니다. 페이지를 새로 고침해 주세요."
                                      + 새로고침 버튼 노출
```

| 시도 횟수 | 대기 시간 |
|-----------|-----------|
| 1차 | 즉시 (0초) |
| 2차 | 3초 후 |
| 3차 | 5초 후 |
| 이후 | 재시도 중단, 사용자에게 새로고침 안내 |

### 5.3 메시지 전송 실패 (WebSocket)

```
STOMP SEND 후 연결 끊김 감지
  └─ Optimistic UI 롤백 (임시 메시지 제거)
  └─ 토스트 "메시지 전송에 실패했습니다."
  └─ 재연결 흐름 시작
```

---

## 6. AI 멘션 에러 처리

### 6.1 AI 에러 흐름 (서버 → 채널 브로드캐스트)

```
Claude API 호출
  ├─ 성공 → AI_RESPONSE 브로드캐스트 (로딩 메시지 교체)
  ├─ API 에러 (5xx) → AI_ERROR 브로드캐스트
  │    └─ 로딩 메시지 교체: "AI 응답 생성에 실패했습니다. 잠시 후 다시 시도해 주세요."
  └─ 타임아웃 (10초) → AI_ERROR 브로드캐스트
       └─ 로딩 메시지 교체: "AI 응답 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요."
```

### 6.2 AI 에러 메시지 스타일

AI 에러 메시지는 일반 AI 응답과 동일한 파란 배경 박스로 표시하되, 텍스트 앞에 ⚠️ 아이콘 추가.

```
[🤖]  ChatterAI
┌─────────────────────────────────────────────────┐
│  ⚠️ AI 응답 생성에 실패했습니다.                 │
│     잠시 후 다시 시도해 주세요.                  │
└─────────────────────────────────────────────────┘
```

### 6.3 AI 사용량 초과 프론트 처리

```javascript
// AI 멘션 전송 전 클라이언트 사전 검증
const checkAiUsage = async () => {
  const { data } = await userApi.getAiUsage();
  if (data.remainCount <= 0) {
    uiStore.showToast('error',
      `오늘의 AI 사용 횟수(${data.limitCount}회)를 모두 사용했습니다.`
    );
    return false;
  }
  return true;
};

// 메시지 전송 핸들러
const handleSend = async () => {
  const hasAiMention = content.value.includes('@AI');
  if (hasAiMention) {
    const canUseAi = await checkAiUsage();
    if (!canUseAi) return;   // 전송 중단
  }
  // ... 메시지 전송 로직
};
```

> **이중 검증**: 프론트(1차) + 서버(2차) 모두 검증.  
> 클라이언트 우회 시도를 서버에서 `AI_USAGE_EXCEEDED (429)` 로 차단한다.
