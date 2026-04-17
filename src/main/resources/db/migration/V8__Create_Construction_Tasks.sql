-- 1. Create the transactional construction queue
CREATE TABLE construction_tasks (
                                    id UUID PRIMARY KEY,
                                    profile_id UUID NOT NULL,
                                    building_type VARCHAR(50) NOT NULL,
                                    target_level INT NOT NULL,
                                    start_time TIMESTAMP NOT NULL,
                                    completion_time TIMESTAMP NOT NULL,

    -- Corrected to singular 'player_profile' to match your V3 schema
                                    CONSTRAINT fk_profile_construction FOREIGN KEY (profile_id) REFERENCES player_profile(id),
                                    CONSTRAINT unique_build_per_type UNIQUE (profile_id, building_type)
);

-- 2. Add hunting lodges to the existing structures table
ALTER TABLE player_structures
    ADD COLUMN hunting_lodges INT DEFAULT 0 NOT NULL;