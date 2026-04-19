-- V17: Formalize the Admin UI access flag
ALTER TABLE player_account
    ADD COLUMN is_admin BOOLEAN NOT NULL DEFAULT FALSE;