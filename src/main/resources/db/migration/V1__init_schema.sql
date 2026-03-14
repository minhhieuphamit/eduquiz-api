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

-- 1. Roles
CREATE TABLE roles (
    id                  UUID PRIMARY KEY,
    name                VARCHAR(50) UNIQUE NOT NULL,
    allow_registration  BOOLEAN DEFAULT true,
    created_at          TIMESTAMP DEFAULT now(),
    updated_at          TIMESTAMP DEFAULT now()
);

-- Insert Default Roles
INSERT INTO roles (id, name, allow_registration) VALUES ('00000000-0000-6000-8000-000000000001', 'STUDENT', true);
INSERT INTO roles (id, name, allow_registration) VALUES ('00000000-0000-6000-8000-000000000002', 'TEACHER', true);
INSERT INTO roles (id, name, allow_registration) VALUES ('00000000-0000-6000-8000-000000000003', 'ADMIN', false);

-- 2. Users
CREATE TABLE users (
    id              UUID PRIMARY KEY,
    email           VARCHAR(255) UNIQUE NOT NULL,
    password        VARCHAR(255) NOT NULL,
    first_name      VARCHAR(255) NOT NULL,
    last_name       VARCHAR(255) NOT NULL,
    dob             DATE,
    phone_number    VARCHAR(20),
    role_id         UUID NOT NULL REFERENCES roles(id),
    is_active       BOOLEAN DEFAULT false,
    email_verified  BOOLEAN DEFAULT false,
    created_at      TIMESTAMP DEFAULT now(),
    updated_at      TIMESTAMP DEFAULT now(),
    token_invalidated_at TIMESTAMP
);

-- 3. Email Verifications (OTP)
CREATE TABLE email_verifications (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    otp_code        VARCHAR(6) NOT NULL,
    expires_at      TIMESTAMP NOT NULL,
    verified        BOOLEAN DEFAULT false,
    attempts        INT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT now()
);

-- 4. Refresh Tokens
CREATE TABLE refresh_tokens (
    id              UUID PRIMARY KEY,
    token           VARCHAR(500) UNIQUE NOT NULL,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expiry_date     TIMESTAMP NOT NULL,
    revoked         BOOLEAN DEFAULT false
);

-- 5. Subjects
CREATE TABLE subjects (
    id                      UUID PRIMARY KEY,
    name                    VARCHAR(100) UNIQUE NOT NULL,
    description             TEXT,
    icon                    VARCHAR(255),
    default_duration_minutes INT DEFAULT 50,
    created_at              TIMESTAMP DEFAULT now(),
    updated_at              TIMESTAMP DEFAULT now()
);

-- 6. Chapters
CREATE TABLE chapters (
    id              UUID PRIMARY KEY,
    subject_id      UUID NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    order_index     INT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT now(),
    updated_at      TIMESTAMP DEFAULT now()
);

-- 7. Questions
CREATE TABLE questions (
    id              UUID PRIMARY KEY,
    chapter_id      UUID NOT NULL REFERENCES chapters(id) ON DELETE CASCADE,
    content         TEXT NOT NULL,
    type            VARCHAR(50) NOT NULL, -- SINGLE_CHOICE, MULTI_CHOICE, etc.
    difficulty      VARCHAR(20) NOT NULL, -- EASY, MEDIUM, HARD
    created_at      TIMESTAMP DEFAULT now(),
    updated_at      TIMESTAMP DEFAULT now()
);

-- 8. Exams
CREATE TABLE exams (
    id                  UUID PRIMARY KEY,
    title               VARCHAR(255) NOT NULL,
    description         TEXT,
    creator_id          UUID NOT NULL REFERENCES users(id),
    subject_id          UUID NOT NULL REFERENCES subjects(id),
    duration_minutes    INT NOT NULL,
    random_mode         VARCHAR(20) NOT NULL, -- FULL_RANDOM, POOL_RANDOM, MANUAL
    created_at          TIMESTAMP DEFAULT now(),
    updated_at          TIMESTAMP DEFAULT now()
);

-- 9. Exam Questions (Many-to-Many)
CREATE TABLE exam_questions (
    exam_id         UUID NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    question_id     UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    PRIMARY KEY (exam_id, question_id)
);

-- 10. Exam Rooms
CREATE TABLE exam_rooms (
    id              UUID PRIMARY KEY,
    exam_id         UUID NOT NULL REFERENCES exams(id),
    room_code       VARCHAR(10) UNIQUE NOT NULL,
    start_time      TIMESTAMP NOT NULL,
    end_time        TIMESTAMP NOT NULL,
    status          VARCHAR(20) NOT NULL, -- SCHEDULED, OPEN, IN_PROGRESS, CLOSED
    created_at      TIMESTAMP DEFAULT now(),
    updated_at      TIMESTAMP DEFAULT now()
);

-- 11. Room Participants
CREATE TABLE room_participants (
    room_id         UUID NOT NULL REFERENCES exam_rooms(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    exam_id         UUID REFERENCES exams(id), -- Đề thi cụ thể HS nhận được
    joined_at       TIMESTAMP DEFAULT now(),
    PRIMARY KEY (room_id, user_id)
);

-- 12. Exam Sessions
CREATE TABLE exam_sessions (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    exam_id         UUID NOT NULL REFERENCES exams(id),
    room_id         UUID REFERENCES exam_rooms(id), -- Nullable for practice
    start_time      TIMESTAMP DEFAULT now(),
    end_time        TIMESTAMP,
    score           DECIMAL(5, 2),
    status          VARCHAR(20) NOT NULL, -- IN_PROGRESS, SUBMITTED, GRADED
    created_at      TIMESTAMP DEFAULT now(),
    updated_at      TIMESTAMP DEFAULT now()
);

-- 13. Exam Answers
CREATE TABLE exam_answers (
    id              UUID PRIMARY KEY,
    session_id      UUID NOT NULL REFERENCES exam_sessions(id) ON DELETE CASCADE,
    question_id     UUID NOT NULL REFERENCES questions(id),
    answer_content  TEXT,
    is_correct      BOOLEAN,
    created_at      TIMESTAMP DEFAULT now()
);

-- 14. Audit Logs
CREATE TABLE audit_logs (
    id              UUID PRIMARY KEY,
    user_id         UUID REFERENCES users(id) ON DELETE SET NULL,
    action          VARCHAR(255) NOT NULL,
    entity_name     VARCHAR(100),
    entity_id       UUID,
    details         JSONB,
    created_at      TIMESTAMP DEFAULT now()
);

-- Indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_email_verifications_user_id ON email_verifications(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_chapters_subject_id ON chapters(subject_id);
CREATE INDEX idx_questions_chapter_id ON questions(chapter_id);
CREATE INDEX idx_exams_creator_id ON exams(creator_id);
CREATE INDEX idx_exam_rooms_room_code ON exam_rooms(room_code);
CREATE INDEX idx_exam_sessions_user_id ON exam_sessions(user_id);
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);