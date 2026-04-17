-- ================================================
-- ChatterAI Database Schema
-- ================================================
-- 실행 전 chatterai DB가 없으면 아래 주석 해제 후 실행:
-- CREATE DATABASE IF NOT EXISTS chatterai
--   DEFAULT CHARACTER SET utf8mb4
--   DEFAULT COLLATE utf8mb4_unicode_ci;

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
    KEY idx_channels_created_by (created_by),
    CONSTRAINT fk_channels_creator FOREIGN KEY (created_by)
        REFERENCES users (id)
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
-- (AI_LOADING은 DB 저장 안 함 — 브로드캐스트 전용)
-- ================================================
CREATE TABLE messages (
    id            BIGINT     NOT NULL AUTO_INCREMENT,
    channel_id    BIGINT         NULL,
    dm_room_id    BIGINT         NULL,
    user_id       BIGINT         NULL,
    content       TEXT       NOT NULL,
    type          ENUM('TEXT', 'FILE', 'AI_RESPONSE', 'AI_ERROR') NOT NULL DEFAULT 'TEXT',
    is_deleted    TINYINT(1) NOT NULL DEFAULT 0,
    is_ai_message TINYINT(1) NOT NULL DEFAULT 0,
    created_at    DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    KEY idx_messages_channel (channel_id, created_at),
    KEY idx_messages_dm (dm_room_id, created_at),
    KEY idx_messages_user (user_id),
    CONSTRAINT fk_msg_channel FOREIGN KEY (channel_id)
        REFERENCES channels (id) ON DELETE CASCADE,
    CONSTRAINT fk_msg_dm FOREIGN KEY (dm_room_id)
        REFERENCES dm_rooms (id) ON DELETE CASCADE,
    CONSTRAINT fk_msg_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ================================================
-- 7. channels.notice_message_id FK
-- (순환 FK: channels ↔ messages 해소를 위해 ALTER로 추가)
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
