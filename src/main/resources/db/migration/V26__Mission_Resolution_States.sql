-- V26: Add resolution persistence to active missions
ALTER TABLE active_mission ADD COLUMN is_resolved BOOLEAN DEFAULT FALSE;
ALTER TABLE active_mission ADD COLUMN was_successful BOOLEAN DEFAULT FALSE;