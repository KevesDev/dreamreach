-- Issue 7: Add rolled rarity to player instances to support Rarity-on-Purchase
ALTER TABLE player_character ADD COLUMN rolled_rarity VARCHAR(20);