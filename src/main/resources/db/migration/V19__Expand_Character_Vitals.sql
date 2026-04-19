-- V19: Add missing vitals and description fields for the detailed character sheet
ALTER TABLE character_template
    ADD COLUMN description TEXT;

ALTER TABLE player_character
    ADD COLUMN description TEXT,
ADD COLUMN max_hit_dice INTEGER NOT NULL DEFAULT 1,
ADD COLUMN mana_slots_json TEXT,
ADD COLUMN last_rest_tick TIMESTAMP WITH TIME ZONE;