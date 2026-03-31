-- =====================================================
-- V6: Add audit fields + shared flag to questions
-- =====================================================

-- 1. Audit columns
ALTER TABLE questions ADD COLUMN created_by UUID REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE questions ADD COLUMN updated_by UUID REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX idx_questions_created_by ON questions(created_by);

-- 2. Shared flag: khi is_shared = true, tất cả giáo viên đều thấy câu hỏi này
ALTER TABLE questions ADD COLUMN is_shared BOOLEAN DEFAULT false NOT NULL;

CREATE INDEX idx_questions_is_shared ON questions(is_shared) WHERE is_shared = true;
