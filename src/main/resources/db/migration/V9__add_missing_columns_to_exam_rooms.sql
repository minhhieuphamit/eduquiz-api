-- ══════════════════════════════════════════════════════
-- V9: Add missing columns to exam_rooms
-- ══════════════════════════════════════════════════════

ALTER TABLE exam_rooms ADD COLUMN IF NOT EXISTS title VARCHAR(255);
-- Update existing rows to have a title if they don't already
UPDATE exam_rooms SET title = 'Untitled Room' WHERE title IS NULL;
-- Enforce NOT NULL as per JPA entity
ALTER TABLE exam_rooms ALTER COLUMN title SET NOT NULL;

ALTER TABLE exam_rooms ADD COLUMN IF NOT EXISTS created_by UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_exam_rooms_created_by'
          AND table_name = 'exam_rooms'
    ) THEN
        ALTER TABLE exam_rooms ADD CONSTRAINT fk_exam_rooms_created_by
            FOREIGN KEY (created_by) REFERENCES users(id);
    END IF;
END $$;

ALTER TABLE exam_rooms ADD COLUMN IF NOT EXISTS max_students INTEGER;
ALTER TABLE exam_rooms ADD COLUMN IF NOT EXISTS duration_minutes INTEGER;
