-- 1. Create the new relational table for individual building instances
CREATE TABLE building_instances (
                                    id UUID PRIMARY KEY,
                                    profile_id UUID NOT NULL REFERENCES player_profile(id) ON DELETE CASCADE,
                                    building_type VARCHAR(50) NOT NULL,
                                    level INT NOT NULL DEFAULT 1,
                                    assigned_workers INT NOT NULL DEFAULT 0
);

-- 2. Add the bakers column to the population table so we can track the profession
ALTER TABLE player_population ADD COLUMN bakers INT NOT NULL DEFAULT 0;

-- 3. Drop the old aggregate structures table
DROP TABLE player_structures;