-- V25: Add Stat Priority for Guided RNG Generation
ALTER TABLE character_template ADD COLUMN stat_priority_json TEXT DEFAULT '["STR", "CON", "DEX", "WIS", "CHA", "INT"]';