-- V22: Add dynamic duration to quest templates
ALTER TABLE quest_template ADD COLUMN duration_hours INT DEFAULT 4;