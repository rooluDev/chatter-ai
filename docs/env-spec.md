# ⚙️ Env Spec

## ChatterAI — 실시간 채팅 + AI 어시스턴트 플랫폼

> 이 문서는 환경 변수, 설정 파일 위치, 로컬 실행 방법을 정의한다.  
> `application-local.yml`과 `.env.local`은 Git에 포함하지 않는다.  
> Claude Code는 이 문서를 참조해 설정 파일을 생성한다.

---

## 목차

1. [설정 파일 목록](#1-설정-파일-목록)
2. [백엔드 설정](#2-백엔드-설정)
3. [프론트엔드 설정](#3-프론트엔드-설정)
4. [로컬 실행 방법](#4-로컬-실행-방법)
5. [사전 요구사항 및 설치](#5-사전-요구사항-및-설치)

---

## 1. 설정 파일 목록

| 파일                    | 위치                          | Git 포함 | 용도                                                              |
| ----------------------- | ----------------------------- | -------- | ----------------------------------------------------------------- |
| `application.yml`       | `backend/src/main/resources/` | ✅       | 공통 설정 (포트, JPA, WebSocket, Redis, 파일, JWT 만료시간, CORS) |
| `application-local.yml` | `backend/src/main/resources/` | ❌       | DB 접속정보, JWT Secret, Redis 비밀번호, Claude API Key           |
| `.env`                  | `frontend/`                   | ✅       | API Base URL (기본값)                                             |
| `.env.local`            | `frontend/`                   | ❌       | 로컬 환경 오버라이드 (필요 시)                                    |

> `application-local.yml`이 없으면 백엔드가 기동되지 않는다.  
> `.gitignore`에 반드시 포함되어 있는지 확인한다.

---

## 2. 백엔드 설정

### 2.1 application.yml (Git 포함 — 민감 정보 없음)

```yaml
# backend/src/main/resources/application.yml

spring:
    profiles:
        active: local # 기본 프로필: local

    # JPA
    jpa:
        hibernate:
            ddl-auto: validate # 운영: validate / 개발 초기: create 후 변경
        show-sql: false # 운영: false / 디버깅 시: true
        properties:
            hibernate:
                format_sql: true
                default_batch_fetch_size: 100

    # WebSocket (STOMP)
    websocket:
        allowed-origins: ${ALLOWED_ORIGINS:http://localhost:5173}

    # 파일 업로드
    servlet:
        multipart:
            max-file-size: 20MB
            max-request-size: 110MB # 파일 5개 × 20MB + 여유분

    # Redis
    data:
        redis:
            host: ${REDIS_HOST:localhost}
            port: ${REDIS_PORT:6379}
            # password는 application-local.yml에서 주입

# 서버 포트
server:
    port: 8080

# JWT
jwt:
    access-token-expiry: 1800000 # 30분 (ms)
    refresh-token-expiry: 604800 # 7일 (초, Redis TTL용)

# 파일 저장 경로
file:
    upload-dir: ./uploads
    serve-path: /api/files/uploads/**

# AI
ai:
    daily-usage-limit: 20 # 일반 회원 일일 AI 사용 제한 횟수
    timeout-seconds: 10 # Claude API 타임아웃 (초)
    context-message-count: 10 # AI에 전달하는 최근 메시지 수

# CORS
cors:
    allowed-origins: ${ALLOWED_ORIGINS:http://localhost:5173}

# 로깅
logging:
    level:
        com.chatterai: INFO
        org.springframework.web.socket: INFO
        org.springframework.messaging: INFO
```

---

### 2.2 application-local.yml (Git 제외 — 직접 생성 필요)

> 이 파일은 로컬 환경에서 직접 생성한다. 아래 템플릿을 복사해 값을 채운다.

```yaml
# backend/src/main/resources/application-local.yml
# ⚠️ 이 파일은 절대 Git에 커밋하지 않는다.

spring:
    datasource:
        url: jdbc:mysql://localhost:3306/chatterai?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
        username: root
        password: [db-password]
        driver-class-name: com.mysql.cj.jdbc.Driver

    data:
        redis:

jwt:
    secret: asdasdgrsdggsfasdfasdfsadfadfsghsfhdgfbfsdfsdfsdfadgfd
    # 생성 예시: openssl rand -base64 32

ai:
    claude:
        api-key: [api-key]
        model: claude-sonnet-4-5
```

**각 값 발급/생성 방법**

| 항목                  | 방법                                                                           |
| --------------------- | ------------------------------------------------------------------------------ |
| `{MySQL_비밀번호}`    | 로컬 MySQL 설치 시 설정한 root 비밀번호                                        |
| `{Redis_비밀번호}`    | Redis 비밀번호 설정 안 했으면 해당 줄 삭제                                     |
| `{JWT_SECRET_KEY}`    | 터미널: `openssl rand -base64 32` (32자 이상 필수)                             |
| `{Anthropic_API_Key}` | [console.anthropic.com](https://console.anthropic.com) → API Keys → Create Key |

---

### 2.3 .gitignore (백엔드)

```gitignore
# backend/.gitignore (또는 루트 .gitignore에 추가)

# 민감 정보 설정 파일
**/application-local.yml
**/application-*.yml
!**/application.yml          # 공통 설정은 포함

# 업로드 파일
uploads/

# 빌드 산출물
build/
.gradle/
*.jar
```

---

### 2.4 Spring Boot 프로파일 활성화 방식

`application.yml`에서 `spring.profiles.active: local`로 기본 프로파일을 지정한다.  
Spring Boot는 `application-{profile}.yml`을 자동으로 병합 로드한다.

```
application.yml (공통) + application-local.yml (로컬) → 병합된 설정으로 실행
```

운영 환경에서는 환경 변수로 프로파일을 교체한다.

```bash
# 운영 배포 시
SPRING_PROFILES_ACTIVE=prod java -jar chatterai.jar
```

---

## 3. 프론트엔드 설정

### 3.1 .env (Git 포함 — 기본값)

```env
# frontend/.env

VITE_API_BASE_URL=http://localhost:8080/api
VITE_WS_URL=ws://localhost:8080/ws
```

---

### 3.2 .env.local (Git 제외 — 필요 시 생성)

로컬에서 기본값과 다른 포트나 URL을 사용하는 경우에만 생성한다.

```env
# frontend/.env.local
# ⚠️ 이 파일은 Git에 커밋하지 않는다.

VITE_API_BASE_URL=http://localhost:8080/api
VITE_WS_URL=ws://localhost:8080/ws
```

---

### 3.3 vite.config.js (Git 포함)

개발 서버 프록시 설정. 이 설정이 없으면 CORS 에러 및 이미지 깨짐 발생.

```javascript
// frontend/vite.config.js

import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import path from "path";

export default defineConfig({
    plugins: [vue()],
    resolve: {
        alias: {
            "@": path.resolve(__dirname, "./src"),
        },
    },
    server: {
        port: 5173,
        proxy: {
            // REST API 프록시
            "/api": {
                target: "http://localhost:8080",
                changeOrigin: true,
            },
            // WebSocket 프록시 (STOMP)
            "/ws": {
                target: "http://localhost:8080",
                changeOrigin: true,
                ws: true, // ← WebSocket 프록시 활성화 필수
            },
        },
    },
});
```

> **`ws: true` 누락 주의**: 이 옵션이 없으면 WebSocket 연결이 프록시를 통과하지 못해 개발 환경에서 STOMP 연결이 실패한다.

---

### 3.4 .gitignore (프론트엔드)

```gitignore
# frontend/.gitignore

# 민감 정보
.env.local
.env.*.local

# 의존성
node_modules/

# 빌드 산출물
dist/

# 에디터
.vscode/
.idea/
```

---

## 4. 로컬 실행 방법

### 4.1 최초 환경 구성 (한 번만)

```bash
# 1. DB 스키마 생성
mysql -u root -p chatterai < db-schema.sql

# 2. 초기 데이터 삽입 (관리자 계정 + 기본 채널)
mysql -u root -p chatterai < db-seed.sql

# 3. 백엔드 로컬 설정 파일 생성
#    backend/src/main/resources/application-local.yml 을 직접 작성
#    (2.2 섹션 템플릿 참조)

# 4. 프론트엔드 의존성 설치
cd frontend && npm install
```

### 4.2 일반 실행

```bash
# 터미널 1 — Redis 실행 (이미 실행 중이면 생략)
redis-server

# 터미널 2 — 백엔드
cd backend
./gradlew bootRun

# 터미널 3 — 프론트엔드
cd frontend
npm run dev
```

### 4.3 접속 URL

| 서비스      | URL                       |
| ----------- | ------------------------- |
| Frontend    | http://localhost:5173     |
| Backend API | http://localhost:8080/api |
| WebSocket   | ws://localhost:8080/ws    |
| MySQL       | localhost:3306/chatterai  |
| Redis       | localhost:6379            |

### 4.4 초기 관리자 계정

| 항목     | 값           |
| -------- | ------------ |
| 아이디   | `admin`      |
| 비밀번호 | `Admin1234!` |
| 역할     | `ADMIN`      |

---

## 5. 사전 요구사항 및 설치

### 5.1 필수 도구

| 도구    | 최소 버전          | 설치 확인                |
| ------- | ------------------ | ------------------------ |
| Java    | 17 이상            | `java -version`          |
| Gradle  | 8.x (Wrapper 포함) | `./gradlew -v`           |
| Node.js | 18 이상            | `node -v`                |
| npm     | 9 이상             | `npm -v`                 |
| MySQL   | 8.x                | `mysql --version`        |
| Redis   | 7.x                | `redis-server --version` |

### 5.2 도구별 설치 방법 (macOS 기준)

```bash
# Java 17 (Homebrew)
brew install openjdk@17
echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc

# Node.js 18 (nvm 사용 권장)
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash
nvm install 18
nvm use 18

# MySQL 8
brew install mysql
brew services start mysql
mysql_secure_installation   # 초기 비밀번호 설정

# Redis 7
brew install redis
brew services start redis
```

### 5.3 DB 초기화

```bash
# MySQL 접속
mysql -u root -p

# DB 생성 (db-schema.sql 실행 전 필요 시)
CREATE DATABASE chatterai
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
exit;

# 스키마 실행
mysql -u root -p chatterai < db-schema.sql

# 초기 데이터 실행
mysql -u root -p chatterai < db-seed.sql
```

### 5.4 Redis 연결 확인

```bash
# Redis CLI 접속
redis-cli

# 연결 확인
ping   # PONG 응답 확인

# 기존 테스트 데이터 전체 삭제 (개발 중 필요 시)
flushall
```

### 5.5 환경별 변수 요약표

| 변수           | 개발(로컬)                 | 운영                    |
| -------------- | -------------------------- | ----------------------- |
| DB URL         | `localhost:3306/chatterai` | 운영 DB 주소            |
| Redis Host     | `localhost`                | 운영 Redis 주소         |
| JWT Secret     | 로컬 생성 문자열           | 운영 Secret (별도 관리) |
| Claude API Key | 개발용 Key                 | 운영용 Key              |
| CORS Origin    | `http://localhost:5173`    | 운영 도메인             |
| WS URL         | `ws://localhost:8080/ws`   | `wss://도메인/ws`       |
| HTTPS          | 미적용 (HTTP)              | Nginx + Let's Encrypt   |
