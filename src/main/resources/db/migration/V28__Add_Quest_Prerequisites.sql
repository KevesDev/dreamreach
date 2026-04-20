-- V28: Add progression gating to Quest Templates
ALTER TABLE quest_template ADD COLUMN min_keep_level INT NOT NULL DEFAULT 1;
ALTER TABLE quest_template ADD COLUMN prerequisite_quest_id UUID REFERENCES quest_template(id) ON DELETE SET NULL;