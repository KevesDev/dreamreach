-- V20: Create Quest Templates and Party schemas
CREATE TABLE quest_template (
                                id UUID PRIMARY KEY,
                                type VARCHAR(50) NOT NULL,
                                title VARCHAR(100) NOT NULL,
                                description TEXT,
                                target_stats_json TEXT,
                                advantage_classes_json TEXT,
                                disadvantage_classes_json TEXT
);

CREATE TABLE party (
                       id UUID PRIMARY KEY,
                       owner_id UUID NOT NULL,
                       slot1_id UUID,
                       slot2_id UUID,
                       slot3_id UUID,
                       slot4_id UUID,
                       slot5_id UUID,
                       CONSTRAINT fk_party_owner FOREIGN KEY (owner_id) REFERENCES player_profile(id),
                       CONSTRAINT fk_party_slot1 FOREIGN KEY (slot1_id) REFERENCES player_character(id),
                       CONSTRAINT fk_party_slot2 FOREIGN KEY (slot2_id) REFERENCES player_character(id),
                       CONSTRAINT fk_party_slot3 FOREIGN KEY (slot3_id) REFERENCES player_character(id),
                       CONSTRAINT fk_party_slot4 FOREIGN KEY (slot4_id) REFERENCES player_character(id),
                       CONSTRAINT fk_party_slot5 FOREIGN KEY (slot5_id) REFERENCES player_character(id)
);