-- V27: Add Royal Ledger to track offline and kingdom events
CREATE TABLE ledger_entry (
                              id UUID PRIMARY KEY,
                              profile_id UUID NOT NULL REFERENCES player_profile(id),
                              event_timestamp TIMESTAMP NOT NULL,
                              category VARCHAR(50) NOT NULL,
                              message TEXT NOT NULL
);