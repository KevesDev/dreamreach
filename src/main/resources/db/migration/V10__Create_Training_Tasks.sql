-- V10: Create the Training Tasks table to support sequential peasant profession training.
-- Uses Cascade delete so if a profile is wiped, their queue is wiped too.
CREATE TABLE training_tasks (
                                id UUID PRIMARY KEY,
                                profile_id UUID NOT NULL REFERENCES player_profile(id) ON DELETE CASCADE,
                                profession_type VARCHAR(50) NOT NULL,
                                start_time TIMESTAMP WITH TIME ZONE NOT NULL,
                                completion_time TIMESTAMP WITH TIME ZONE NOT NULL
);