-- V2__Initial_Schema.sql
-- Exact translation of current Java Entities to PostgreSQL schema.

-- 1. Create the base authentication account (PlayerAccount)
CREATE TABLE player_account (
                                id UUID PRIMARY KEY,
                                email VARCHAR(255) NOT NULL UNIQUE,
                                password VARCHAR(255) NOT NULL,
                                is_enabled BOOLEAN NOT NULL DEFAULT FALSE,
-- V2__Initial_Schema.sql
-- Exact translation of current Java Entities to PostgreSQL schema.

-- 1. Create the base authentication account (PlayerAccount)
                                CREATE TABLE player_account (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    consecutive_logins INT NOT NULL DEFAULT 0,
    last_login_date TIMESTAMP(6) WITH TIME ZONE,
    last_claim_date TIMESTAMP(6) WITH TIME ZONE,

    -- Embedded PendingReward fields
    reward_type VARCHAR(50),
    amount INT,
    reason VARCHAR(255)
);

-- 2. Create Alliances (Must exist before Profiles can join them)
CREATE TABLE alliance (
                          id UUID PRIMARY KEY,
                          name VARCHAR(100) NOT NULL UNIQUE,
                          tag VARCHAR(10) NOT NULL UNIQUE,
                          level INT NOT NULL,
                          description VARCHAR(255),
                          is_alliance_pvp_enabled BOOLEAN NOT NULL DEFAULT FALSE
);

-- 3. Create the public profile (PlayerProfile)
CREATE TABLE player_profile (
                                id UUID PRIMARY KEY,
                                account_id UUID NOT NULL UNIQUE REFERENCES player_account(id) ON DELETE CASCADE,
                                alliance_id UUID REFERENCES alliance(id) ON DELETE SET NULL,
                                display_name VARCHAR(50) NOT NULL UNIQUE,
                                is_personal_pvp_enabled BOOLEAN NOT NULL DEFAULT FALSE
);

-- 4. Create the Verification Token table
CREATE TABLE verification_token (
                                    id UUID PRIMARY KEY,
                                    token VARCHAR(255) NOT NULL UNIQUE,
                                    account_id UUID NOT NULL REFERENCES player_account(id) ON DELETE CASCADE,
                                    expiry_date TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

-- 5. Create Player Resources (@OneToOne with Profile)
CREATE TABLE player_resources (
                                  id UUID PRIMARY KEY,
                                  profile_id UUID NOT NULL UNIQUE REFERENCES player_profile(id) ON DELETE CASCADE,
                                  food INT NOT NULL DEFAULT 0,
                                  wood INT NOT NULL DEFAULT 0,
                                  stone INT NOT NULL DEFAULT 0,
                                  gold INT NOT NULL DEFAULT 0,
                                  gems INT NOT NULL DEFAULT 0
);

-- 6. Create Player Population (@OneToOne with Profile)
CREATE TABLE player_population (
                                   id UUID PRIMARY KEY,
                                   profile_id UUID NOT NULL UNIQUE REFERENCES player_profile(id) ON DELETE CASCADE,
                                   happiness INT NOT NULL DEFAULT 50,
                                   idle_peasants INT NOT NULL DEFAULT 0,
                                   hunters INT NOT NULL DEFAULT 0,
                                   woodcutters INT NOT NULL DEFAULT 0,
                                   stoneworkers INT NOT NULL DEFAULT 0
);

-- 7. Create Player Structures (@OneToOne with Profile)
CREATE TABLE player_structures (
                                   id UUID PRIMARY KEY,
                                   profile_id UUID NOT NULL UNIQUE REFERENCES player_profile(id) ON DELETE CASCADE,
                                   houses INT NOT NULL DEFAULT 0,
                                   towers INT NOT NULL DEFAULT 0,
                                   bakeries INT NOT NULL DEFAULT 0
);

-- 8. Create Character Templates (Read-only blueprints)
CREATE TABLE character_template (
                                    id UUID PRIMARY KEY,
                                    name VARCHAR(100) NOT NULL UNIQUE,
                                    rarity VARCHAR(255) NOT NULL,
                                    dnd_class VARCHAR(255) NOT NULL,
                                    base_str INT NOT NULL DEFAULT 10,
                                    base_dex INT NOT NULL DEFAULT 10,
                                    base_con INT NOT NULL DEFAULT 10,
                                    base_int INT NOT NULL DEFAULT 10,
                                    base_wis INT NOT NULL DEFAULT 10,
                                    base_cha INT NOT NULL DEFAULT 10
);

-- 9. Create Player Character Instances (@ManyToOne with Profile and Template)
CREATE TABLE player_character (
                                  id UUID PRIMARY KEY,
                                  owner_id UUID NOT NULL REFERENCES player_profile(id) ON DELETE CASCADE,
                                  template_id UUID NOT NULL REFERENCES character_template(id) ON DELETE RESTRICT,
                                  current_level INT NOT NULL DEFAULT 1,
                                  current_xp INT NOT NULL DEFAULT 0,
                                  bonus_str INT NOT NULL DEFAULT 0,
                                  bonus_dex INT NOT NULL DEFAULT 0,
                                  bonus_con INT NOT NULL DEFAULT 0,
                                  bonus_int INT NOT NULL DEFAULT 0,
                                  bonus_wis INT NOT NULL DEFAULT 0,
                                  bonus_cha INT NOT NULL DEFAULT 0
);