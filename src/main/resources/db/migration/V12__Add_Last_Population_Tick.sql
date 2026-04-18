-- V12: Add Last Population Tick for the 15-minute RNG engine
ALTER TABLE player_population
    ADD COLUMN last_population_tick TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;