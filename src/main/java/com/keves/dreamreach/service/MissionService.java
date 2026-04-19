package com.keves.dreamreach.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keves.dreamreach.config.GameQuestConfig;
import com.keves.dreamreach.dto.ActiveMissionResponse;
import com.keves.dreamreach.entity.*;
import com.keves.dreamreach.exception.ResourceNotFoundException;
import com.keves.dreamreach.repository.*;
import com.keves.dreamreach.util.DndMathUtility;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MissionService {

    private final QuestTemplateRepository questRepo;
    private final PlayerCharacterRepository charRepo;
    private final PartyRepository partyRepo;
    private final PlayerProfileRepository profileRepo;
    private final ActiveMissionRepository activeMissionRepo;
    private final GameQuestConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

    public MissionService(QuestTemplateRepository questRepo, PlayerCharacterRepository charRepo,
                          PartyRepository partyRepo, PlayerProfileRepository profileRepo,
                          ActiveMissionRepository activeMissionRepo, GameQuestConfig config) {
        this.questRepo = questRepo;
        this.charRepo = charRepo;
        this.partyRepo = partyRepo;
        this.profileRepo = profileRepo;
        this.activeMissionRepo = activeMissionRepo;
        this.config = config;
    }

    public List<QuestTemplate> getAllQuests() {
        return questRepo.findAll();
    }

    /**
     * Executes the complex math logic to determine a party's chance of success.
     */
    public int calculateSuccessChance(List<UUID> characterIds, UUID questId) {
        if (characterIds == null || characterIds.isEmpty()) return 0;
        QuestTemplate quest = questRepo.findById(questId).orElseThrow(() -> new ResourceNotFoundException("Quest not found."));
        List<PlayerCharacter> partyMembers = charRepo.findAllById(characterIds);

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

        double baseChance = calculateBaseChance(targetStats, partyMembers);
        int advCount = 0;
        int disCount = 0;

        for (PlayerCharacter pc : partyMembers) {
            String dndClass = pc.getTemplate().getDndClass().name();
            if (advClasses.contains(dndClass)) advCount++;
            if (disClasses.contains(dndClass)) disCount++;
        }

        double finalSuccessPct = baseChance + (advCount * config.getClassAdvantageBonus() * 100.0) - (disCount * config.getClassDisadvantagePenalty() * 100.0);
        return (int) Math.round(Math.max(config.getMinimumSuccessChance() * 100.0, Math.min(config.getMaximumSuccessChance() * 100.0, finalSuccessPct)));
    }

    private double calculateBaseChance(Map<String, Integer> targetStats, List<PlayerCharacter> partyMembers) {
        double totalFulfillment = 0;
        int statCount = targetStats.size();
        if (statCount == 0) return 100.0;

        for (Map.Entry<String, Integer> entry : targetStats.entrySet()) {
            String stat = entry.getKey().toUpperCase();
            int target = entry.getValue();
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
            double fulfillment = target > 0 ? ((double) partyTotal / target) * 100.0 : 100.0;
            totalFulfillment += Math.min(100.0, fulfillment);
        }
        return totalFulfillment / statCount;
    }

    @Transactional
    public void saveParty(String displayName, List<UUID> characterIds) {
        PlayerProfile profile = profileRepo.findByDisplayName(displayName).orElseThrow(() -> new ResourceNotFoundException("Profile not found."));
        Party party = partyRepo.findByOwnerId(profile.getId()).stream().findFirst().orElse(new Party());
        party.setOwner(profile);
        party.setSlot1Id(null); party.setSlot2Id(null); party.setSlot3Id(null); party.setSlot4Id(null); party.setSlot5Id(null);

        if (characterIds != null) {
            if (!characterIds.isEmpty()) party.setSlot1Id(characterIds.get(0));
            if (characterIds.size() > 1) party.setSlot2Id(characterIds.get(1));
            if (characterIds.size() > 2) party.setSlot3Id(characterIds.get(2));
            if (characterIds.size() > 3) party.setSlot4Id(characterIds.get(3));
            if (characterIds.size() > 4) party.setSlot5Id(characterIds.get(4));
        }
        partyRepo.save(party);
    }

    /**
     * Dispatches a party on an active expedition.
     */
    @Transactional
    public void dispatchParty(String displayName, UUID questId, List<UUID> characterIds) {
        if (characterIds == null || characterIds.isEmpty()) throw new IllegalArgumentException("Cannot dispatch an empty party.");
        PlayerProfile profile = profileRepo.findByDisplayName(displayName).orElseThrow(() -> new ResourceNotFoundException("Profile not found."));
        QuestTemplate quest = questRepo.findById(questId).orElseThrow(() -> new ResourceNotFoundException("Quest not found."));

        List<PlayerCharacter> characters = charRepo.findAllById(characterIds);
        for (PlayerCharacter pc : characters) {
            if (!"IDLE".equalsIgnoreCase(pc.getStatus())) {
                throw new IllegalStateException("Character " + pc.getTemplate().getName() + " is not available for dispatch.");
            }
            pc.setStatus("MISSION");
        }
        charRepo.saveAll(characters);

        // Snap a fresh Party instance specifically for this expedition
        Party expeditionParty = new Party();
        expeditionParty.setOwner(profile);
        if (characterIds.size() > 0) expeditionParty.setSlot1Id(characterIds.get(0));
        if (characterIds.size() > 1) expeditionParty.setSlot2Id(characterIds.get(1));
        if (characterIds.size() > 2) expeditionParty.setSlot3Id(characterIds.get(2));
        if (characterIds.size() > 3) expeditionParty.setSlot4Id(characterIds.get(3));
        if (characterIds.size() > 4) expeditionParty.setSlot5Id(characterIds.get(4));
        partyRepo.save(expeditionParty);

        int chance = calculateSuccessChance(characterIds, questId);

        ActiveMission activeMission = new ActiveMission();
        activeMission.setParty(expeditionParty);
        activeMission.setQuestTemplate(quest);
        activeMission.setSuccessChance(chance);
        activeMission.setDispatchTime(Instant.now());
        // For Sprint 5 testing, hardcoded 4 hours.
        activeMission.setEndTime(Instant.now().plus(4, ChronoUnit.HOURS));

        activeMissionRepo.save(activeMission);
    }

    /**
     * Triggers the DM Resolution Engine, then returns remaining active expeditions.
     */
    @Transactional
    public List<ActiveMissionResponse> getActiveMissions(String displayName) {
        PlayerProfile profile = profileRepo.findByDisplayName(displayName).orElseThrow(() -> new ResourceNotFoundException("Profile not found."));

        resolveCompletedMissions(profile); // Lazy Evaluation Roll

        List<ActiveMission> remainingMissions = activeMissionRepo.findByPartyOwnerId(profile.getId());

        return remainingMissions.stream().map(mission -> {
            List<PlayerCharacter> partyMembers = getPartyMembers(mission.getParty());
            List<ActiveMissionResponse.CharacterSnippet> snippets = partyMembers.stream()
                    .map(pc -> ActiveMissionResponse.CharacterSnippet.builder()
                            .characterId(pc.getId())
                            .name(pc.getTemplate().getName())
                            .portraitUrl(pc.getTemplate().getPortraitUrl())
                            .flavorQuipsJson(pc.getTemplate().getFlavorQuips())
                            .build())
                    .collect(Collectors.toList());

            return ActiveMissionResponse.builder()
                    .missionId(mission.getId())
                    .questTitle(mission.getQuestTemplate().getTitle())
                    .questType(mission.getQuestTemplate().getType().name())
                    .successChance(mission.getSuccessChance())
                    .dispatchTimeEpoch(mission.getDispatchTime().toEpochMilli())
                    .endTimeEpoch(mission.getEndTime().toEpochMilli())
                    .partyMembers(snippets)
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * The DM Engine. Rolls success, divides XP, applies consequences.
     */
    @Transactional
    public void resolveCompletedMissions(PlayerProfile profile) {
        List<ActiveMission> missions = activeMissionRepo.findByPartyOwnerId(profile.getId());
        Instant now = Instant.now();

        for (ActiveMission mission : missions) {
            if (now.isBefore(mission.getEndTime())) continue;

            QuestTemplate quest = mission.getQuestTemplate();
            List<PlayerCharacter> characters = getPartyMembers(mission.getParty());

            int roll = random.nextInt(100) + 1; // 1-100
            boolean success = roll <= mission.getSuccessChance();

            if (success) {
                profile.setGold(profile.getGold() + (quest.getRewardGold() != null ? quest.getRewardGold() : 0));
                profile.setGems(profile.getGems() + (quest.getRewardGems() != null ? quest.getRewardGems() : 0));
                profile.setFood(profile.getFood() + (quest.getRewardFood() != null ? quest.getRewardFood() : 0));
                profile.setWood(profile.getWood() + (quest.getRewardWood() != null ? quest.getRewardWood() : 0));
                profile.setStone(profile.getStone() + (quest.getRewardStone() != null ? quest.getRewardStone() : 0));

                int xpShare = (quest.getBaseExp() != null && !characters.isEmpty()) ? (quest.getBaseExp() / characters.size()) : 0;

                for (PlayerCharacter pc : characters) {
                    pc.setCurrentXp(pc.getCurrentXp() + xpShare);
                    processLevelUp(pc);
                    // 10% HP damage for success
                    int dmg = Math.max(1, (int) (pc.getMaxHp() * 0.10));
                    pc.setCurrentHp(Math.max(1, pc.getCurrentHp() - dmg));
                    pc.setStatus("IDLE");
                }
            } else {
                for (PlayerCharacter pc : characters) {
                    // 90% HP damage for failure
                    int dmg = Math.max(1, (int) (pc.getMaxHp() * 0.90));
                    pc.setCurrentHp(Math.max(1, pc.getCurrentHp() - dmg));
                    if (pc.getCurrentHp() == 1) {
                        pc.setStatus("KO");
                    } else {
                        pc.setStatus("IDLE");
                    }
                }
            }

            charRepo.saveAll(characters);
            profileRepo.save(profile);
            activeMissionRepo.delete(mission);
        }
    }

    private void processLevelUp(PlayerCharacter pc) {
        int newLevel = DndMathUtility.calculateLevelFromXp(pc.getCurrentXp());
        if (newLevel > pc.getCurrentLevel()) {
            int levelDiff = newLevel - pc.getCurrentLevel();
            int avgHd = (pc.getTemplate().getHitDieType() / 2) + 1;
            int conMod = DndMathUtility.calculateModifier(pc.getTotalConstitution());

            int hpGain = Math.max(1, avgHd + conMod) * levelDiff;

            pc.setMaxHp(pc.getMaxHp() + hpGain);
            pc.setCurrentHp(pc.getCurrentHp() + hpGain);
            pc.setMaxHitDice(pc.getMaxHitDice() + levelDiff);
            pc.setCurrentLevel(newLevel);
        }
    }

    private List<PlayerCharacter> getPartyMembers(Party party) {
        List<UUID> ids = new ArrayList<>();
        if (party.getSlot1Id() != null) ids.add(party.getSlot1Id());
        if (party.getSlot2Id() != null) ids.add(party.getSlot2Id());
        if (party.getSlot3Id() != null) ids.add(party.getSlot3Id());
        if (party.getSlot4Id() != null) ids.add(party.getSlot4Id());
        if (party.getSlot5Id() != null) ids.add(party.getSlot5Id());
        return charRepo.findAllById(ids);
    }
}