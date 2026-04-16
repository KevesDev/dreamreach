-- V5__Update_Verification_Token.sql
-- Corrects the verification code column, aligns boolean mapping, and fixes timezone mapping.

-- 1. Correct the verification code column to match entity mapping and remove invalid unique constraint
ALTER TABLE verification_token
DROP COLUMN token;

ALTER TABLE verification_token
    ADD COLUMN code VARCHAR(6) NOT NULL;

-- 2. Hibernate strips 'is' from boolean properties without explicit @Column names
ALTER TABLE player_account
    RENAME COLUMN is_enabled TO enabled;

-- 3. Hibernate maps LocalDateTime to TIMESTAMP WITHOUT TIME ZONE
ALTER TABLE verification_token
ALTER COLUMN expiry_date TYPE TIMESTAMP(6) WITHOUT TIME ZONE;