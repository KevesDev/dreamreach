-- V24: Add published toggle to control visibility on the global Adventurer's Board
ALTER TABLE quest_template ADD COLUMN is_published BOOLEAN DEFAULT TRUE;