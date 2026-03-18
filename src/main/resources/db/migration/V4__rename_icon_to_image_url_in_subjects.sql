-- ══════════════════════════════════════════════════════
-- V4: Đổi cột icon → image_url trong bảng subjects
-- ══════════════════════════════════════════════════════

ALTER TABLE subjects RENAME COLUMN icon TO image_url;

ALTER TABLE subjects ALTER COLUMN image_url TYPE VARCHAR(500);
