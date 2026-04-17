# 🗄️ DB Schema

## ChatterAI — 실시간 채팅 + AI 어시스턴트 플랫폼

> 이 문서는 테이블 설계, 컬럼 명세, 인덱스, DDL을 정의한다.  
> 구현 시 이 문서의 DDL을 기준으로 스키마를 생성한다.

---

## 목차

1. [ERD 개요](#1-erd-개요)
2. [테이블 명세](#2-테이블-명세)
3. [테이블 관계 정리](#3-테이블-관계-정리)
4. [DDL (전체 SQL)](#4-ddl-전체-sql)
5. [초기 데이터 (Seed)](#5-초기-데이터-seed)
6. [설계 주요 사항](#6-설계-주요-사항)

---

## 1. ERD 개요

```
users
 ├── channel_participants (N:M users ↔ channels)
 ├── messages (1:N users → messages)
 ├── dm_participants (N:M users ↔ dm_rooms)
 └── reactions (1:N users → reactions)

channels
 ├── channel_participants
 └── messages (channel_id FK)

dm_rooms
 ├── dm_participants
 └── messages (dm_room_id FK)

messages
 ├── attachments (1:N messages → attachments)
 └── reactions (1:N messages → reactions)
```

---

## 2. 테이블 명세

---

### 2.1 users

사용자 계정 정보

| 컬럼 | 타입 | NOT NULL | 기본값 | 설명 |
|------|------|----------|--------|------|
| id | BIGINT | ✅ | AUTO_INCREMENT | PK |
| username | VARCHAR(20) | ✅ | — | 로그인 아이디 (영문+숫자 4~20자) |
| nickname | VARCHAR(20) | ✅ | — | 채팅 표시명 (2~20자) |
| password | VARCHAR(255) | ✅ | — | BCrypt 해시 비밀번호 |
| role | ENUM('USER','ADMIN') | ✅ | 'USER' | 권한 |
| is_active | TINYINT(1) | ✅ | 1 | 활성 여부 (0: 비활성) |
| created_at | DATETIME | ✅ | CURRENT_TIMESTAMP | 가입일시 |
| updated_at | DATETIME | ✅ | CURRENT_TIMESTAMP ON UPDATE | 수정일시 |

**인덱스**
- `PK`: id
- `UNIQUE`: username
- `UNIQUE`: nickname

---

### 2.2 channels

채팅 채널

| 컬럼 | 타입 | NOT NULL | 기본값 | 설명 |
|------|------|----------|--------|------|
| id | BIGINT | ✅ | AUTO_INCREMENT | PK |
| name | VARCHAR(30) | ✅ | — | 채널명 (2~30자) |
| description | VARCHAR(255) | ❌ | NULL | 채널 설명 |
| is_private | TINYINT(1) | ✅ | 0 | 비공개 여부 |
| notice_message_id | BIGINT | ❌ | NULL | 공지 메시지 FK (messages.id) |
| created_by | BIGINT | ✅ | — | 생성한 관리자 FK (users.id) |
| created_at | DATETIME | ✅ | CURRENT_TIMESTAMP | 생성일시 |
| updated_at | DATETIME | ✅ | CURRENT_TIMESTAMP ON UPDATE | 수정일시 |

**인덱스**
- `PK`: id
- `UNIQUE`: name
- `FK`: created_by → users.id
- `FK`: notice_message_id → messages.id (SET NULL on DELETE)

---

### 2.3 channel_participants

채널 참여자 (users ↔ channels N:M)

| 컬럼 | 타입 | NOT NULL | 기본값 | 설명 |
|------|------|----------|--------|------|
| id | BIGINT | ✅ | AUTO_INCREMENT | PK |
| channel_id | BIGINT | ✅ | — | FK (channels.id) |
| user_id | BIGINT | ✅ | — | FK (users.id) |
| joined_at | DATETIME | ✅ | CURRENT_TIMESTAMP | 참여일시 |

**인덱스**
- `PK`: id
- `UNIQUE`: (channel_id, user_id) — 동일 채널 중복 참여 방지
- `FK`: channel_id → channels.id (CASCADE on DELETE)
- `FK`: user_id → users.id (CASCADE on DELETE)

---

### 2.4 dm_rooms

1:1 다이렉트 메시지 방

| 컬럼 | 타입 | NOT NULL | 기본값 | 설명 |
|------|------|----------|--------|------|
| id | BIGINT | ✅ | AUTO_INCREMENT | PK |
| created_at | DATETIME | ✅ | CURRENT_TIMESTAMP | 생성일시 |

---

### 2.5 dm_participants

DM 방 참여자 (users ↔ dm_rooms N:M, 실제로는 항상 2명)

| 컬럼 | 타입 | NOT NULL | 기본값 | 설명 |
|------|------|----------|--------|------|
| id | BIGINT | ✅ | AUTO_INCREMENT | PK |
| dm_room_id | BIGINT | ✅ | — | FK (dm_rooms.id) |
| user_id | BIGINT | ✅ | — | FK (users.id) |
| joined_at | DATETIME | ✅ | CURRENT_TIMESTAMP | 참여일시 |

**인덱스**
- `PK`: id
- `UNIQUE`: (dm_room_id, user_id)
- `FK`: dm_room_id → dm_rooms.id (CASCADE on DELETE)
- `FK`: user_id → users.id (CASCADE on DELETE)
- `INDEX`: user_id — 특정 사용자의 DM 목록 조회 최적화

---

### 2.6 messages

채널 및 DM 메시지 (단일 테이블)

| 컬럼 | 타입 | NOT NULL | 기본값 | 설명 |
|------|------|----------|--------|------|
| id | BIGINT | ✅ | AUTO_INCREMENT | PK |
| channel_id | BIGINT | ❌ | NULL | FK (channels.id), 채널 메시지 |
| dm_room_id | BIGINT | ❌ | NULL | FK (dm_rooms.id), DM 메시지 |
| user_id | BIGINT | ❌ | NULL | FK (users.id), NULL이면 AI 메시지 |
| content | TEXT | ✅ | — | 메시지 내용 |
| type | ENUM('TEXT','FILE','AI_LOADING','AI_RESPONSE','AI_ERROR') | ✅ | 'TEXT' | 메시지 유형 |
| is_deleted | TINYINT(1) | ✅ | 0 | 소프트 삭제 여부 |
| is_ai_message | TINYINT(1) | ✅ | 0 | AI 응답 여부 |
| created_at | DATETIME | ✅ | CURRENT_TIMESTAMP | 전송일시 |
| updated_at | DATETIME | ✅ | CURRENT_TIMESTAMP ON UPDATE | 수정일시 |

**인덱스**
- `PK`: id
- `FK`: channel_id → channels.id (CASCADE on DELETE)
- `FK`: dm_room_id → dm_rooms.id (CASCADE on DELETE)
- `FK`: user_id → users.id (SET NULL on DELETE)
- `INDEX`: (channel_id, created_at DESC) — 채널 메시지 최신순 조회
- `INDEX`: (dm_room_id, created_at DESC) — DM 메시지 최신순 조회

**설계 주의**
- `channel_id`와 `dm_room_id` 중 하나만 NOT NULL이어야 한다 (Application 레이어에서 검증)
- `is_deleted = 1`인 메시지는 content를 `"삭제된 메시지입니다."`로 교체하여 반환
- `type = AI_LOADING`인 메시지는 DB에 저장하지 않는다 (브로드캐스트 전용)

---

### 2.7 attachments

메시지 첨부파일

| 컬럼 | 타입 | NOT NULL | 기본값 | 설명 |
|------|------|----------|--------|------|
| id | BIGINT | ✅ | AUTO_INCREMENT | PK |
| message_id | BIGINT | ✅ | — | FK (messages.id) |
| file_url | VARCHAR(512) | ✅ | — | 파일 서빙 URL |
| file_name | VARCHAR(255) | ✅ | — | 원본 파일명 |
| file_size | BIGINT | ✅ | — | 파일 크기 (bytes) |
| file_type | ENUM('IMAGE','FILE') | ✅ | 'FILE' | 이미지 여부 |
| sort_order | INT | ✅ | 0 | 첨부 순서 |
| created_at | DATETIME | ✅ | CURRENT_TIMESTAMP | 업로드일시 |

**인덱스**
- `PK`: id
- `FK`: message_id → messages.id (CASCADE on DELETE)
- `INDEX`: message_id — 메시지별 첨부파일 조회

---

### 2.8 reactions

메시지 이모지 반응

| 컬럼 | 타입 | NOT NULL | 기본값 | 설명 |
|------|------|----------|--------|------|
| id | BIGINT | ✅ | AUTO_INCREMENT | PK |
| message_id | BIGINT | ✅ | — | FK (messages.id) |
| user_id | BIGINT | ✅ | — | FK (users.id) |
| emoji | VARCHAR(10) | ✅ | — | 이모지 문자 (ex. 👍) |
| created_at | DATETIME | ✅ | CURRENT_TIMESTAMP | 반응일시 |

**인덱스**
- `PK`: id
- `UNIQUE`: (message_id, user_id, emoji) — 동일 이모지 중복 반응 방지
- `FK`: message_id → messages.id (CASCADE on DELETE)
- `FK`: user_id → users.id (CASCADE on DELETE)
- `INDEX`: message_id — 메시지별 반응 목록 조회

---

## 3. 테이블 관계 정리

| 관계 | 설명 | FK 제약 |
|------|------|---------|
| users → messages | 1:N (한 사용자가 여러 메시지) | SET NULL on DELETE |
| channels → messages | 1:N | CASCADE on DELETE |
| dm_rooms → messages | 1:N | CASCADE on DELETE |
| messages → attachments | 1:N | CASCADE on DELETE |
| messages → reactions | 1:N | CASCADE on DELETE |
| users ↔ channels | N:M (channel_participants) | CASCADE on DELETE |
| users ↔ dm_rooms | N:M (dm_participants) | CASCADE on DELETE |
| channels → messages (notice) | 1:1 (notice_message_id) | SET NULL on DELETE |

---

## 4. DDL (전체 SQL)

```sql
-- ================================================
-- ChatterAI Database Schema
-- ================================================

CREATE DATABASE IF NOT EXISTS chatterai
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE chatterai;

-- ================================================
-- 1. users
-- ================================================
CREATE TABLE users (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    username   VARCHAR(20)  NOT NULL,
    nickname   VARCHAR(20)  NOT NULL,
    password   VARCHAR(255) NOT NULL,
    role       ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER',
    is_active  TINYINT(1)   NOT NULL DEFAULT 1,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_users_username (username),
    UNIQUE KEY uq_users_nickname (nickname)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ================================================
-- 2. channels
-- ================================================
CREATE TABLE channels (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    name              VARCHAR(30)  NOT NULL,
    description       VARCHAR(255)     NULL,
    is_private        TINYINT(1)   NOT NULL DEFAULT 0,
    notice_message_id BIGINT           NULL,
    created_by        BIGINT       NOT NULL,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_channels_name (name),
    KEY idx_channels_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ================================================
-- 3. channel_participants
-- ================================================
CREATE TABLE channel_participants (
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    channel_id BIGINT   NOT NULL,
    user_id    BIGINT   NOT NULL,
    joined_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_channel_participants (channel_id, user_id),
    KEY idx_channel_participants_user (user_id),
    CONSTRAINT fk_cp_channel FOREIGN KEY (channel_id)
        REFERENCES channels (id) ON DELETE CASCADE,
    CONSTRAINT fk_cp_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ================================================
-- 4. dm_rooms
-- ================================================
CREATE TABLE dm_rooms (
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ================================================
-- 5. dm_participants
-- ================================================
CREATE TABLE dm_participants (
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    dm_room_id BIGINT   NOT NULL,
    user_id    BIGINT   NOT NULL,
    joined_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_dm_participants (dm_room_id, user_id),
    KEY idx_dm_participants_user (user_id),
    CONSTRAINT fk_dp_dm_room FOREIGN KEY (dm_room_id)
        REFERENCES dm_rooms (id) ON DELETE CASCADE,
    CONSTRAINT fk_dp_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ================================================
-- 6. messages
-- ================================================
CREATE TABLE messages (
    id            BIGINT   NOT NULL AUTO_INCREMENT,
    channel_id    BIGINT       NULL,
    dm_room_id    BIGINT       NULL,
    user_id       BIGINT       NULL,
    content       TEXT     NOT NULL,
    type          ENUM('TEXT', 'FILE', 'AI_RESPONSE', 'AI_ERROR') NOT NULL DEFAULT 'TEXT',
    is_deleted    TINYINT(1) NOT NULL DEFAULT 0,
    is_ai_message TINYINT(1) NOT NULL DEFAULT 0,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    KEY idx_messages_channel (channel_id, created_at DESC),
    KEY idx_messages_dm (dm_room_id, created_at DESC),
    KEY idx_messages_user (user_id),
    CONSTRAINT fk_msg_channel FOREIGN KEY (channel_id)
        REFERENCES channels (id) ON DELETE CASCADE,
    CONSTRAINT fk_msg_dm FOREIGN KEY (dm_room_id)
        REFERENCES dm_rooms (id) ON DELETE CASCADE,
    CONSTRAINT fk_msg_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ================================================
-- 7. channels.notice_message_id FK (messages 생성 후 추가)
-- ================================================
ALTER TABLE channels
    ADD CONSTRAINT fk_channels_notice
        FOREIGN KEY (notice_message_id)
        REFERENCES messages (id) ON DELETE SET NULL;

-- ================================================
-- 8. attachments
-- ================================================
CREATE TABLE attachments (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    message_id BIGINT        NOT NULL,
    file_url   VARCHAR(512)  NOT NULL,
    file_name  VARCHAR(255)  NOT NULL,
    file_size  BIGINT        NOT NULL,
    file_type  ENUM('IMAGE', 'FILE') NOT NULL DEFAULT 'FILE',
    sort_order INT           NOT NULL DEFAULT 0,
    created_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    KEY idx_attachments_message (message_id),
    CONSTRAINT fk_att_message FOREIGN KEY (message_id)
        REFERENCES messages (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ================================================
-- 9. reactions
-- ================================================
CREATE TABLE reactions (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    message_id BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    emoji      VARCHAR(10) NOT NULL,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_reactions (message_id, user_id, emoji),
    KEY idx_reactions_message (message_id),
    CONSTRAINT fk_react_message FOREIGN KEY (message_id)
        REFERENCES messages (id) ON DELETE CASCADE,
    CONSTRAINT fk_react_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## 5. 초기 데이터 (Seed)

```sql
-- ================================================
-- db-seed.sql
-- 관리자 계정 + 기본 채널 초기 데이터
-- ================================================

USE chatterai;

-- 관리자 계정
-- 비밀번호: Admin1234! (BCrypt 해시)
INSERT INTO users (username, nickname, password, role)
VALUES (
    'admin',
    '관리자',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'ADMIN'
);

-- 기본 채널 3개
INSERT INTO channels (name, description, is_private, created_by) VALUES
    ('일반', '모두를 위한 일반 채팅 채널입니다.', 0, 1),
    ('공지', '공지사항 채널입니다.', 0, 1),
    ('자유', '자유롭게 이야기하는 채널입니다.', 0, 1);

-- 관리자를 기본 채널에 참여
INSERT INTO channel_participants (channel_id, user_id) VALUES
    (1, 1),
    (2, 1),
    (3, 1);
```

---

## 6. 설계 주요 사항

### 6.1 messages 단일 테이블 설계

채널 메시지와 DM 메시지를 별도 테이블로 분리하지 않고 단일 `messages` 테이블로 통합했다.

- `channel_id NOT NULL + dm_room_id NULL` → 채널 메시지
- `channel_id NULL + dm_room_id NOT NULL` → DM 메시지
- Application 레이어에서 둘 중 하나만 값이 있음을 보장한다

**장점**: 첨부파일·이모지 반응 테이블을 공유할 수 있어 중복 테이블 불필요  
**단점**: 둘 다 NULL인 잘못된 데이터가 DB 레벨에서 허용됨 → Service 레이어에서 검증 필수

### 6.2 메시지 소프트 삭제

`is_deleted = 1`로 처리하며 content를 응답 시 `"삭제된 메시지입니다."`로 교체한다.  
하드 삭제 시 이전 메시지 흐름이 끊어지는 UX 문제와 첨부파일 참조 무결성 문제가 발생한다.

### 6.3 AI 메시지의 user_id

AI가 보낸 메시지는 `user_id = NULL`, `is_ai_message = 1`로 저장한다.  
AI를 별도 users 레코드로 만들지 않아 회원 관리 로직의 오염을 방지한다.

### 6.4 AI_LOADING 타입 미저장

`type = AI_LOADING`인 로딩 메시지는 DB에 저장하지 않고 WebSocket 브로드캐스트 전용으로만 사용한다.  
히스토리 로드 시 로딩 메시지가 남아 있으면 안 되기 때문이다.

### 6.5 순환 FK 처리 (channels ↔ messages)

`channels.notice_message_id`와 `messages.channel_id`가 서로를 참조하는 순환 FK가 발생한다.  
MySQL에서 순환 FK는 테이블 생성 순서 문제로 에러가 발생하므로, `channels` 테이블 생성 후 `messages` 테이블 생성, 이후 `ALTER TABLE`로 `notice_message_id FK`를 추가하는 순서로 해결한다.

### 6.6 복합 인덱스 설계

```sql
KEY idx_messages_channel (channel_id, created_at DESC)
KEY idx_messages_dm (dm_room_id, created_at DESC)
```

메시지 조회의 대부분은 `WHERE channel_id = ? ORDER BY created_at DESC LIMIT 20` 패턴이다.  
`channel_id`를 선두 컬럼으로 두고 `created_at`을 두 번째 컬럼으로 복합 인덱스를 구성해 정렬 비용을 제거한다.

### 6.7 삭제 시 처리 방식 요약

| 삭제 대상 | 연관 데이터 처리 |
|-----------|-----------------|
| users 삭제 | messages.user_id → SET NULL (메시지는 유지) |
| users 삭제 | channel_participants → CASCADE 삭제 |
| users 삭제 | reactions → CASCADE 삭제 |
| channels 삭제 | messages → CASCADE 삭제 |
| channels 삭제 | channel_participants → CASCADE 삭제 |
| messages 삭제 | attachments → CASCADE 삭제 |
| messages 삭제 | reactions → CASCADE 삭제 |
| messages 삭제 (공지) | channels.notice_message_id → SET NULL |
