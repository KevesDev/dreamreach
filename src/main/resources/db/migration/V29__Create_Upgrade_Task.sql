-- V29: Create explicit Upgrade Tasks table to handle upgrading existing building instances
CREATE TABLE upgrade_task (
                              id UUID PRIMARY KEY,
                              profile_id UUID NOT NULL REFERENCES player_profile(id),
                              building_instance_id UUID NOT NULL REFERENCES building_instances(id),
                              target_level INT NOT NULL,
                              start_time TIMESTAMP WITH TIME ZONE NOT NULL,
                              completion_time TIMESTAMP WITH TIME ZONE NOT NULL
);