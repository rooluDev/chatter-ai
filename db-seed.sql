-- ================================================
-- ChatterAI Seed Data
-- 관리자 계정 + 기본 채널 초기 데이터
-- ================================================
-- 실행 전 db-schema.sql이 먼저 적용되어 있어야 한다.

USE chatterai;

-- ================================================
-- 1. 관리자 계정
-- username: admin / password: Admin1234! (BCrypt)
-- ================================================
INSERT INTO users (username, nickname, password, role)
VALUES (
    'admin',
    '관리자',
    '$2a$10$xHoegEZnXQjMylrHV30YM.dZo6y.RbuiBZCl3fweHzB7bdciUkuoG',
    'ADMIN'
);

-- ================================================
-- 2. 기본 채널 3개
-- ================================================
INSERT INTO channels (name, description, is_private, created_by) VALUES
    ('일반', '모두를 위한 일반 채팅 채널입니다.', 0, 1),
    ('공지', '공지사항 채널입니다.', 0, 1),
    ('자유', '자유롭게 이야기하는 채널입니다.', 0, 1);

-- ================================================
-- 3. 관리자를 기본 채널 3개에 참여
-- ================================================
INSERT INTO channel_participants (channel_id, user_id) VALUES
    (1, 1),
    (2, 1),
    (3, 1);
