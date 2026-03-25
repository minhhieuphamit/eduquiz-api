-- ══════════════════════════════════════════════════════
-- V5: Add question_options table + enhance chapters/questions/exams
-- ══════════════════════════════════════════════════════

-- 1. Chapters: thêm description, is_active (soft delete)
ALTER TABLE chapters ADD COLUMN description TEXT;
ALTER TABLE chapters ADD COLUMN is_active BOOLEAN DEFAULT true;

-- 2. Questions: thêm explanation, is_active (soft delete)
ALTER TABLE questions ADD COLUMN explanation TEXT;
ALTER TABLE questions ADD COLUMN is_active BOOLEAN DEFAULT true;

-- 3. Question Options (đáp án A, B, C, D)
CREATE TABLE question_options (
    id              UUID PRIMARY KEY,
    question_id     UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    label           VARCHAR(5) NOT NULL,
    content         TEXT NOT NULL,
    is_correct      BOOLEAN DEFAULT false,
    order_index     INT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT now(),
    updated_at      TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_question_options_question_id ON question_options(question_id);

-- 4. Exams: thêm year, exam_type, is_active (soft delete)
ALTER TABLE exams ADD COLUMN year INT;
ALTER TABLE exams ADD COLUMN exam_type VARCHAR(30) DEFAULT 'PRACTICE';
ALTER TABLE exams ADD COLUMN is_active BOOLEAN DEFAULT true;
ALTER TABLE exams ADD COLUMN total_questions INT DEFAULT 0;

CREATE INDEX idx_exams_subject_year ON exams(subject_id, year);
CREATE INDEX idx_exams_exam_type ON exams(exam_type);
