package com.keves.dreamreach.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keves.dreamreach.config.GameEconomyConfig;
import com.keves.dreamreach.dto.CharacterRosterResponse;
import com.keves.dreamreach.entity.CharacterTemplate;
import com.keves.dreamreach.entity.PlayerCharacter;
import com.keves.dreamreach.entity.PlayerProfile;
import com.keves.dreamreach.enums.Rarity;
import com.keves.dreamreach.repository.CharacterTemplateRepository;
import com.keves.dreamreach.repository.PlayerCharacterRepository;
import com.keves.dreamreach.util.DndMathUtility;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The Universal Gacha Engine.
 * Centralizes all RNG, stat generation, and character minting logic.
 */
@Service
public class GachaService {

    private final GameEconomyConfig config;
    private final CharacterTemplateRepository templateRepo;
    private final PlayerCharacterRepository characterRepo;
    private final PlayerCharacterService playerCharacterService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

    public GachaService(GameEconomyConfig config,
                        CharacterTemplateRepository templateRepo,
                        PlayerCharacterRepository characterRepo,
                        PlayerCharacterService playerCharacterService) {
        this.config = config;
        this.templateRepo = templateRepo;
        this.characterRepo = characterRepo;
        this.playerCharacterService = playerCharacterService;
    }

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

        // --- Step 2: Generate Stats and Map them ---
        List<String> priority = getPriorityList(template);
        Map<String, Integer> stats = DndMathUtility.generateHeroStats(priority, pulledRarity);

        PlayerCharacter newCharacter = buildCharacterFromStats(profile, template, pulledRarity, stats);
        newCharacter = characterRepo.save(newCharacter);

        return playerCharacterService.mapToRosterResponse(newCharacter);
    }

    @Transactional
    public PlayerCharacter generateRandomCharacter(PlayerProfile owner) {
        List<CharacterTemplate> allTemplates = templateRepo.findAll();
        if (allTemplates.isEmpty()) throw new IllegalStateException("No character templates found.");

        CharacterTemplate template = allTemplates.get(random.nextInt(allTemplates.size()));
        return instantiateFromTemplate(owner, template);
    }

    @Transactional
    public PlayerCharacter instantiateFromTemplate(PlayerProfile owner, CharacterTemplate template) {
        List<String> priority = getPriorityList(template);
        Map<String, Integer> stats = DndMathUtility.generateHeroStats(priority, template.getRarity());

        PlayerCharacter pc = buildCharacterFromStats(owner, template, template.getRarity(), stats);
        return characterRepo.save(pc);
    }

    private PlayerCharacter buildCharacterFromStats(PlayerProfile owner, CharacterTemplate template, Rarity rarity, Map<String, Integer> stats) {
        PlayerCharacter pc = new PlayerCharacter();
        pc.setOwner(owner);
        pc.setTemplate(template);
        pc.setRolledRarity(rarity);

        // Calculate bonuses relative to template baseline
        pc.setBonusStr(stats.get("STR") - template.getBaseStr());
        pc.setBonusDex(stats.get("DEX") - template.getBaseDex());
        pc.setBonusCon(stats.get("CON") - template.getBaseCon());
        pc.setBonusInt(stats.get("INT") - template.getBaseInt());
        pc.setBonusWis(stats.get("WIS") - template.getBaseWis());
        pc.setBonusCha(stats.get("CHA") - template.getBaseCha());

        // HP calculation using 2024 rules
        int conMod = DndMathUtility.calculateModifier(stats.get("CON"));
        int maxHp = DndMathUtility.calculateMaxHp(1, template.getHitDieType(), conMod);
        pc.setMaxHp(maxHp);
        pc.setCurrentHp(maxHp);

        pc.setCurrentLevel(1);
        pc.setCurrentXp(0);
        pc.setStatus("IDLE");
        pc.setMaxHitDice(1);
        pc.setSpentHitDice(0);
        pc.setLastRestTick(Instant.now());
        pc.setDescription(template.getDescription());

        return pc;
    }

    private List<String> getPriorityList(CharacterTemplate template) {
        try {
            return objectMapper.readValue(template.getStatPriorityJson(), new TypeReference<>() {});
        } catch (Exception e) {
            // Default Fallback priority if JSON is malformed
            return List.of("STR", "CON", "DEX", "WIS", "CHA", "INT");
        }
    }
}