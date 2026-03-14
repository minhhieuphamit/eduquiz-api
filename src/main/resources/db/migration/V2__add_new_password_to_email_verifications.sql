-- Add new_password column to email_verifications table for temporary storage
ALTER TABLE email_verifications ADD COLUMN new_password VARCHAR(255);
