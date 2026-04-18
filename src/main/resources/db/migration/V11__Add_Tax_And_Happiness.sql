-- V11: Add Happiness, Tax Bracket, and Last Tax Collection Time to Player Profile
ALTER TABLE player_profile
    ADD COLUMN happiness INTEGER NOT NULL DEFAULT 50,
ADD COLUMN tax_bracket VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
ADD COLUMN last_tax_collection_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;