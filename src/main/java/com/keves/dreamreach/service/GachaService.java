package com.keves.dreamreach.service;

import com.keves.dreamreach.config.GameEconomyConfig;
import com.keves.dreamreach.dto.CharacterRosterResponse;
import com.keves.dreamreach.entity.CharacterTemplate;
import com.keves.dreamreach.entity.PlayerCharacter;
import com.keves.dreamreach.entity.PlayerProfile;
import com.keves.dreamreach.enums.Rarity;
import com.keves.dreamreach.repository.PlayerCharacterRepository;
import com.keves.dreamreach.util.DndMathUtility;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Random;

/**
 * The Universal Gacha Engine.
 * Centralizes all RNG, stat generation, and character minting logic so it can be
 * utilized seamlessly by the Tavern, the Shop, or Event Rewards.
 */
@Service
public class GachaService {

    private final GameEconomyConfig config;
    private final PlayerCharacterRepository playerCharacterRepository;
    private final PlayerCharacterService playerCharacterService;
    private final Random random = new Random();

    public GachaService(GameEconomyConfig config,
                        PlayerCharacterRepository playerCharacterRepository,
                        PlayerCharacterService playerCharacterService) {
        this.config = config;
        this.playerCharacterRepository = playerCharacterRepository;
        this.playerCharacterService = playerCharacterService;
    }

    /**
     * Executes the universal gacha pull logic for a given template.
     * Rolls rarity, generates D&D stats, saves the instance, and returns the DTO.
     */
    @Transactional
    public CharacterRosterResponse pullCharacter(PlayerProfile profile, CharacterTemplate template) {

        // --- Step 1: The Gacha Rarity Roll ---
        double roll = random.nextDouble();
        Rarity pulledRarity;

        if (roll < config.getRarityWeightLegendary()) {
            pulledRarity = Rarity.LEGENDARY;
        } else if (roll < config.getRarityWeightLegendary() + config.getRarityWeightEpic()) {
            pulledRarity = Rarity.EPIC;
        } else if (roll < config.getRarityWeightLegendary() + config.getRarityWeightEpic() + config.getRarityWeightRare()) {
            pulledRarity = Rarity.RARE;
        } else if (roll < config.getRarityWeightLegendary() + config.getRarityWeightEpic() + config.getRarityWeightRare() + config.getRarityWeightUncommon()) {
            pulledRarity = Rarity.UNCOMMON;
        } else {
            pulledRarity = Rarity.COMMON;
        }

        // --- Step 2: Rarity-Influenced Stat Generation ---
        Map<String, Integer> rolledStats = DndMathUtility.generateRolledStats(template.getPrimaryStat(), pulledRarity);

        PlayerCharacter newCharacter = new PlayerCharacter();
        newCharacter.setOwner(profile);
        newCharacter.setTemplate(template);
        newCharacter.setRolledRarity(pulledRarity);

        // Translate generated stat block into additive bonuses over the template baseline
        newCharacter.setBonusStr(rolledStats.get("STR") - template.getBaseStr());
        newCharacter.setBonusDex(rolledStats.get("DEX") - template.getBaseDex());
        newCharacter.setBonusCon(rolledStats.get("CON") - template.getBaseCon());
        newCharacter.setBonusInt(rolledStats.get("INT") - template.getBaseInt());
        newCharacter.setBonusWis(rolledStats.get("WIS") - template.getBaseWis());
        newCharacter.setBonusCha(rolledStats.get("CHA") - template.getBaseCha());

        // Calculate Level 1 Maximum HP based on 2024 rules
        int conMod = DndMathUtility.calculateModifier(rolledStats.get("CON"));
        int maxHp = DndMathUtility.calculateMaxHp(1, template.getHitDieType(), conMod);
        newCharacter.setMaxHp(maxHp);
        newCharacter.setCurrentHp(maxHp);

        newCharacter.setCurrentLevel(1);
        newCharacter.setCurrentXp(0);
        newCharacter.setStatus("IDLE");

        // --- Step 2.5: Setup Base Vitals and Inherit Flavor ---
        newCharacter.setMaxHitDice(1);
        newCharacter.setSpentHitDice(0);
        newCharacter.setLastRestTick(Instant.now());
        newCharacter.setDescription(template.getDescription());

        // --- Step 3: Transaction Completion ---
        newCharacter = playerCharacterRepository.save(newCharacter);
        return playerCharacterService.mapToRosterResponse(newCharacter);
    }
}