package com.keves.dreamreach.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keves.dreamreach.config.GameQuestConfig;
import com.keves.dreamreach.entity.Party;
import com.keves.dreamreach.entity.PlayerCharacter;
import com.keves.dreamreach.entity.PlayerProfile;
import com.keves.dreamreach.entity.QuestTemplate;
import com.keves.dreamreach.exception.ResourceNotFoundException;
import com.keves.dreamreach.repository.PartyRepository;
import com.keves.dreamreach.repository.PlayerCharacterRepository;
import com.keves.dreamreach.repository.PlayerProfileRepository;
import com.keves.dreamreach.repository.QuestTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MissionService {

    private final QuestTemplateRepository questRepo;
    private final PlayerCharacterRepository charRepo;
    private final PartyRepository partyRepo;
    private final PlayerProfileRepository profileRepo;
    private final GameQuestConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MissionService(QuestTemplateRepository questRepo, PlayerCharacterRepository charRepo,
                          PartyRepository partyRepo, PlayerProfileRepository profileRepo,
                          GameQuestConfig config) {
        this.questRepo = questRepo;
        this.charRepo = charRepo;
        this.partyRepo = partyRepo;
        this.profileRepo = profileRepo;
        this.config = config;
    }

    /**
     * Executes the complex math logic to determine a party's chance of success.
     */
    public int calculateSuccessChance(List<UUID> characterIds, UUID questId) {
        if (characterIds == null || characterIds.isEmpty()) return 0;

        QuestTemplate quest = questRepo.findById(questId)
                .orElseThrow(() -> new ResourceNotFoundException("Quest not found."));

        List<PlayerCharacter> partyMembers = charRepo.findAllById(characterIds);

        // --- Phase 1: Parse Hidden Requirements ---
        Map<String, Integer> targetStats = new HashMap<>();
        List<String> advClasses = new ArrayList<>();
        List<String> disClasses = new ArrayList<>();

        try {
            if (quest.getTargetStatsJson() != null && !quest.getTargetStatsJson().isEmpty()) {
                targetStats = objectMapper.readValue(quest.getTargetStatsJson(), new TypeReference<>() {});
            }
            if (quest.getAdvantageClassesJson() != null && !quest.getAdvantageClassesJson().isEmpty()) {
                advClasses = objectMapper.readValue(quest.getAdvantageClassesJson(), new TypeReference<>() {});
            }
            if (quest.getDisadvantageClassesJson() != null && !quest.getDisadvantageClassesJson().isEmpty()) {
                disClasses = objectMapper.readValue(quest.getDisadvantageClassesJson(), new TypeReference<>() {});
            }
        } catch (Exception e) {
            System.err.println("Failed to parse quest JSON constraints: " + e.getMessage());
        }

        // --- Phase 2: Base Stat Fulfillment ---
        double baseChance = calculateBaseChance(targetStats, partyMembers);

        // --- Phase 3: Apply Class Advantages/Disadvantages ---
        int advCount = 0;
        int disCount = 0;

        for (PlayerCharacter pc : partyMembers) {
            String dndClass = pc.getTemplate().getDndClass().name();
            if (advClasses.contains(dndClass)) advCount++;
            if (disClasses.contains(dndClass)) disCount++;
        }

        double finalSuccessPct = baseChance
                + (advCount * config.getClassAdvantageBonus() * 100.0)
                - (disCount * config.getClassDisadvantagePenalty() * 100.0);

        // --- Phase 4: Clamp to Configured Limits ---
        double minPct = config.getMinimumSuccessChance() * 100.0;
        double maxPct = config.getMaximumSuccessChance() * 100.0;

        finalSuccessPct = Math.max(minPct, Math.min(maxPct, finalSuccessPct));

        return (int) Math.round(finalSuccessPct);
    }

    private double calculateBaseChance(Map<String, Integer> targetStats, List<PlayerCharacter> partyMembers) {
        double totalFulfillment = 0;
        int statCount = targetStats.size();

        if (statCount == 0) {
            return 100.0;
        }

        for (Map.Entry<String, Integer> entry : targetStats.entrySet()) {
            String stat = entry.getKey().toUpperCase();
            int target = entry.getValue();
            totalFulfillment += calculateStatFulfillment(stat, target, partyMembers);
        }

        return totalFulfillment / statCount;
    }

    private double calculateStatFulfillment(String stat, int target, List<PlayerCharacter> partyMembers) {
        int partyTotal = 0;

        for (PlayerCharacter pc : partyMembers) {
            switch (stat) {
                case "STR" -> partyTotal += pc.getTotalStrength();
                case "DEX" -> partyTotal += pc.getTotalDexterity();
                case "CON" -> partyTotal += pc.getTotalConstitution();
                case "INT" -> partyTotal += pc.getTotalIntelligence();
                case "WIS" -> partyTotal += pc.getTotalWisdom();
                case "CHA" -> partyTotal += pc.getTotalCharisma();
            }
        }

        // Calculate percentage, capping the benefit at 100% per stat
        double fulfillment = target > 0 ? ((double) partyTotal / target) * 100.0 : 100.0;
        return Math.min(100.0, fulfillment);
    }

    @Transactional
    public void saveParty(String displayName, List<UUID> characterIds) {
        PlayerProfile profile = profileRepo.findByDisplayName(displayName)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found."));

        // Fetch their existing assembly or create a new one
        Party party = partyRepo.findByOwnerId(profile.getId()).stream().findFirst().orElse(new Party());
        party.setOwner(profile);

        // Reset slots
        party.setSlot1Id(null);
        party.setSlot2Id(null);
        party.setSlot3Id(null);
        party.setSlot4Id(null);
        party.setSlot5Id(null);

        // Assign active
        if (characterIds != null) {
            if (!characterIds.isEmpty()) party.setSlot1Id(characterIds.get(0));
            if (characterIds.size() > 1) party.setSlot2Id(characterIds.get(1));
            if (characterIds.size() > 2) party.setSlot3Id(characterIds.get(2));
            if (characterIds.size() > 3) party.setSlot4Id(characterIds.get(3));
            if (characterIds.size() > 4) party.setSlot5Id(characterIds.get(4));
        }

        partyRepo.save(party);
    }
}