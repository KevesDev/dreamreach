-- V23: Implement Mission Lifecycle separation (Board vs Journal)
CREATE TABLE accepted_mission (
                                  id UUID PRIMARY KEY,
                                  profile_id UUID NOT NULL,
                                  quest_template_id UUID NOT NULL,
                                  CONSTRAINT fk_ac_mission_profile FOREIGN KEY (profile_id) REFERENCES player_profile(id),
                                  CONSTRAINT fk_ac_mission_quest FOREIGN KEY (quest_template_id) REFERENCES quest_template(id)
);

CREATE TABLE completed_mission (
                                   id UUID PRIMARY KEY,
                                   profile_id UUID NOT NULL,
                                   quest_template_id UUID NOT NULL,
                                   CONSTRAINT fk_co_mission_profile FOREIGN KEY (profile_id) REFERENCES player_profile(id),
                                   CONSTRAINT fk_co_mission_quest FOREIGN KEY (quest_template_id) REFERENCES quest_template(id)
);