-- V4__Update_Pending_Rewards.sql
-- Correcting the embedded PendingReward fields in player_account to match the Java entity.

ALTER TABLE player_account
DROP COLUMN IF EXISTS reward_type,
    DROP COLUMN IF EXISTS amount,
    DROP COLUMN IF EXISTS reason;

ALTER TABLE player_account
    ADD COLUMN pending_food INT,
    ADD COLUMN pending_wood INT,
    ADD COLUMN pending_stone INT,
    ADD COLUMN pending_gold INT,
    ADD COLUMN pending_summon BOOLEAN,
    ADD COLUMN pending_date DATE;