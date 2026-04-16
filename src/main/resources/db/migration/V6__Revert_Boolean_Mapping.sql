-- V6__Revert_Boolean_Mapping.sql
-- Reverting the incorrect boolean column rename. Spring Boot natively maps 'isEnabled' to 'is_enabled'.

ALTER TABLE player_account RENAME COLUMN enabled TO is_enabled;