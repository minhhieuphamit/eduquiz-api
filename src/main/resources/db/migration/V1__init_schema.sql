-- ══════════════════════════════════════════════════════
-- V1: Initial Schema - EduQuiz
-- ══════════════════════════════════════════════════════

-- TODO: Implement tất cả CREATE TABLE theo document spec
-- Thứ tự: users → email_verifications → refresh_tokens → subjects → chapters
--          → questions → exams → exam_questions → exam_rooms → room_participants
--          → exam_sessions → exam_answers → audit_logs

-- Ví dụ:
-- CREATE TABLE users (
--     id          BIGSERIAL PRIMARY KEY,
--     email       VARCHAR(255) UNIQUE NOT NULL,
--     password    VARCHAR(255) NOT NULL,
--     full_name   VARCHAR(255) NOT NULL,
--     role        VARCHAR(20) NOT NULL,
--     is_active   BOOLEAN DEFAULT false,
--     email_verified BOOLEAN DEFAULT false,
--     created_at  TIMESTAMP DEFAULT now(),
--     updated_at  TIMESTAMP DEFAULT now()
-- );
-- ══════════════════════════════════════════════════════
-- V1: Initial Schema - EduQuiz (Phase 1: Auth tables)
-- ══════════════════════════════════════════════════════

-- 1. Users
CREATE TABLE users (
                       id              BIGSERIAL PRIMARY KEY,
                       email           VARCHAR(255) UNIQUE NOT NULL,
                       password        VARCHAR(255) NOT NULL,
                       full_name       VARCHAR(255) NOT NULL,
                       role            VARCHAR(20) NOT NULL,
                       is_active       BOOLEAN DEFAULT false,
                       email_verified  BOOLEAN DEFAULT false,
                       created_at      TIMESTAMP DEFAULT now(),
                       updated_at      TIMESTAMP DEFAULT now()
);

-- 2. Email Verifications (OTP)
CREATE TABLE email_verifications (
                                     id              BIGSERIAL PRIMARY KEY,
                                     user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                     otp_code        VARCHAR(6) NOT NULL,
                                     expires_at      TIMESTAMP NOT NULL,
                                     verified        BOOLEAN DEFAULT false,
                                     attempts        INT DEFAULT 0,
                                     created_at      TIMESTAMP DEFAULT now()
);

-- 3. Refresh Tokens
CREATE TABLE refresh_tokens (
                                id              BIGSERIAL PRIMARY KEY,
                                token           VARCHAR(500) UNIQUE NOT NULL,
                                user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                expiry_date     TIMESTAMP NOT NULL,
                                revoked         BOOLEAN DEFAULT false
);

-- Indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_email_verifications_user_id ON email_verifications(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);