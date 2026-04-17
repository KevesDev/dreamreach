-- V7__Add_Resource_Ledger_Fields.sql
-- Adds fields to support the state-based resource accrual engine.

ALTER TABLE player_resources
    ADD COLUMN last_update TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN pending_food INT NOT NULL DEFAULT 0,
ADD COLUMN pending_wood INT NOT NULL DEFAULT 0,
ADD COLUMN pending_stone INT NOT NULL DEFAULT 0,
ADD COLUMN pending_gold INT NOT NULL DEFAULT 0;