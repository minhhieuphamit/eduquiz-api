-- V2: Thêm cột token_invalidated_at để hỗ trợ logout invalidate JWT
ALTER TABLE users ADD COLUMN token_invalidated_at TIMESTAMP;
