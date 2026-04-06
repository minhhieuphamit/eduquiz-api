-- ══════════════════════════════════════════════════════
-- V7: Enhance exam_sessions and exam_answers for full exam-taking flow
-- ══════════════════════════════════════════════════════

-- 1. exam_sessions: add tracking columns (IF NOT EXISTS = safe to re-run)
ALTER TABLE exam_sessions ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMP;
ALTER TABLE exam_sessions ADD COLUMN IF NOT EXISTS correct_count INTEGER DEFAULT 0;
ALTER TABLE exam_sessions ADD COLUMN IF NOT EXISTS submission_source VARCHAR(20);
ALTER TABLE exam_sessions ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 0 NOT NULL;

-- 2. exam_answers: add updated_at + unique constraint
ALTER TABLE exam_answers ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'uq_session_question'
          AND table_name = 'exam_answers'
    ) THEN
        ALTER TABLE exam_answers ADD CONSTRAINT uq_session_question
            UNIQUE (session_id, question_id);
    END IF;
END $$;

-- 3. Indexes for performance (IF NOT EXISTS supported in PostgreSQL 9.5+)
CREATE INDEX IF NOT EXISTS idx_exam_sessions_exam_id     ON exam_sessions(exam_id);
CREATE INDEX IF NOT EXISTS idx_exam_sessions_status      ON exam_sessions(status);
CREATE INDEX IF NOT EXISTS idx_exam_sessions_user_exam   ON exam_sessions(user_id, exam_id);
CREATE INDEX IF NOT EXISTS idx_exam_answers_session_id   ON exam_answers(session_id);
CREATE INDEX IF NOT EXISTS idx_exam_answers_question_id  ON exam_answers(question_id);
