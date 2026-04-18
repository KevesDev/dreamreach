-- Issue 6: Tavern System - Schema, Config & Offline Arrival Engine

-- Add base costs to character_template for Tavern purchasing
ALTER TABLE character_template ADD COLUMN base_gold_cost INTEGER NOT NULL DEFAULT 500;
ALTER TABLE character_template ADD COLUMN base_gem_cost INTEGER NOT NULL DEFAULT 50;

-- Add tracking for offline catch-up math
ALTER TABLE player_profile ADD COLUMN last_tavern_check_time TIMESTAMP WITH TIME ZONE;

-- Create the dedicated Recruitment Pool to manage who can appear
CREATE TABLE recruitment_pool (
                                  id UUID PRIMARY KEY,
                                  character_template_id UUID NOT NULL REFERENCES character_template(id),
                                  weight INTEGER NOT NULL DEFAULT 10
);

-- Create the Tavern Listing to track who is currently waiting for a player
CREATE TABLE tavern_listing (
                                id UUID PRIMARY KEY,
                                profile_id UUID NOT NULL UNIQUE REFERENCES player_profile(id),
                                character_template_id UUID NOT NULL REFERENCES character_template(id),
                                arrival_time TIMESTAMP WITH TIME ZONE NOT NULL,
                                expiry_time TIMESTAMP WITH TIME ZONE NOT NULL,
                                gold_cost INTEGER NOT NULL,
                                gem_cost INTEGER NOT NULL
);