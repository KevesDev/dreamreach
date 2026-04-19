-- V21: Expand QuestTemplate with Loot tables and create ActiveMissions schema
ALTER TABLE quest_template ADD COLUMN base_exp INT DEFAULT 0;
ALTER TABLE quest_template ADD COLUMN reward_gold INT DEFAULT 0;
ALTER TABLE quest_template ADD COLUMN reward_gems INT DEFAULT 0;
ALTER TABLE quest_template ADD COLUMN reward_food INT DEFAULT 0;
ALTER TABLE quest_template ADD COLUMN reward_wood INT DEFAULT 0;
ALTER TABLE quest_template ADD COLUMN reward_stone INT DEFAULT 0;

CREATE TABLE active_mission (
                                id UUID PRIMARY KEY,
                                party_id UUID NOT NULL,
                                quest_template_id UUID NOT NULL,
                                success_chance INT NOT NULL,
                                dispatch_time TIMESTAMP WITH TIME ZONE NOT NULL,
                                end_time TIMESTAMP WITH TIME ZONE NOT NULL,
                                CONSTRAINT fk_active_mission_party FOREIGN KEY (party_id) REFERENCES party(id),
                                CONSTRAINT fk_active_mission_quest FOREIGN KEY (quest_template_id) REFERENCES quest_template(id)
);