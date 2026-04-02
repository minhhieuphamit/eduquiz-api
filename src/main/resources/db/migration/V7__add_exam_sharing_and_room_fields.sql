-- ══════════════════════════════════════════════════════
-- V7: Add exam sharing + room management fields
-- ══════════════════════════════════════════════════════

-- 1. Exam sharing: global share flag
ALTER TABLE exams ADD COLUMN is_shared BOOLEAN DEFAULT false;

-- 2. ExamRoom enhancements
ALTER TABLE exam_rooms ADD COLUMN title VARCHAR(255);
ALTER TABLE exam_rooms ADD COLUMN teacher_id UUID REFERENCES users(id);
ALTER TABLE exam_rooms ADD COLUMN max_students INT;

-- 3. Indexes for room queries
CREATE INDEX idx_exam_rooms_teacher_id ON exam_rooms(teacher_id);
CREATE INDEX idx_exam_rooms_status ON exam_rooms(status);
CREATE INDEX idx_exam_sessions_room_id ON exam_sessions(room_id);
CREATE INDEX idx_exam_sessions_status ON exam_sessions(status);
