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
    private final AcceptedMissionRepository acceptedRepo;
    private final CompletedMissionRepository completedRepo;
    private final GameQuestConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

    public MissionService(QuestTemplateRepository questRepo, PlayerCharacterRepository charRepo,
                          PartyRepository partyRepo, PlayerProfileRepository profileRepo,
                          ActiveMissionRepository activeMissionRepo, AcceptedMissionRepository acceptedRepo,
                          CompletedMissionRepository completedRepo, GameQuestConfig config) {
        this.questRepo = questRepo;
        this.charRepo = charRepo;
        this.partyRepo = partyRepo;
        this.profileRepo = profileRepo;
        this.activeMissionRepo = activeMissionRepo;
        this.acceptedRepo = acceptedRepo;
        this.completedRepo = completedRepo;
        this.config = config;
    }

    @Transactional
    public List<QuestTemplate> getAdventurersBoard(String displayName) {
        PlayerProfile profile = profileRepo.findByDisplayName(displayName)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found."));

        Set<UUID> excludedIds = new HashSet<>();
        activeMissionRepo.findByPartyOwnerId(profile.getId()).forEach(m -> excludedIds.add(m.getQuestTemplate().getId()));
        acceptedRepo.findByProfileId(profile.getId()).forEach(a -> excludedIds.add(a.getQuestTemplate().getId()));
        completedRepo.findByProfileId(profile.getId()).forEach(c -> excludedIds.add(c.getQuestTemplate().getId()));

        return questRepo.findAll().stream()
                .filter(q -> q.isPublished() && !excludedIds.contains(q.getId()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void acceptMission(String displayName, UUID questId) {
        PlayerProfile profile = profileRepo.findByDisplayName(displayName)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found."));
        QuestTemplate quest = questRepo.findById(questId)
                .orElseThrow(() -> new ResourceNotFoundException("Quest not found."));

        boolean alreadyAccepted = acceptedRepo.findByProfileIdAndQuestTemplateId(profile.getId(), questId).isPresent();
        if (alreadyAccepted) throw new IllegalStateException("Mission already in your Journal.");

        AcceptedMission accepted = new AcceptedMission();
        accepted.setProfile(profile);
        accepted.setQuestTemplate(quest);
        acceptedRepo.save(accepted);
    }

    @Transactional
    public List<QuestTemplate> getJournal(String displayName) {
        PlayerProfile profile = profileRepo.findByDisplayName(displayName)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found."));
        return acceptedRepo.findByProfileId(profile.getId()).stream()
                .map(AcceptedMission::getQuestTemplate)
                .collect(Collectors.toList());
    }

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
        if (targetStats.isEmpty()) return 100.0;

        double totalFulfillment = 0;
        int statCount = targetStats.size();

        for (Map.Entry<String, Integer> entry : targetStats.entrySet()) {
            String stat = entry.getKey().toUpperCase();
            int target = entry.getValue();
            int partyTotal = getPartyTotalForStat(stat, partyMembers);
            double fulfillment = target > 0 ? ((double) partyTotal / target) * 100.0 : 100.0;
            totalFulfillment += Math.min(100.0, fulfillment);
        }
        return totalFulfillment / statCount;
    }

    private int getPartyTotalForStat(String stat, List<PlayerCharacter> partyMembers) {
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
        return partyTotal;
    }

    private void assignPartySlots(Party party, List<UUID> characterIds) {
        party.setSlot1Id(null);
        party.setSlot2Id(null);
        party.setSlot3Id(null);
        party.setSlot4Id(null);
        party.setSlot5Id(null);

        if (characterIds != null) {
            if (!characterIds.isEmpty()) party.setSlot1Id(characterIds.get(0));
            if (characterIds.size() > 1) party.setSlot2Id(characterIds.get(1));
            if (characterIds.size() > 2) party.setSlot3Id(characterIds.get(2));
            if (characterIds.size() > 3) party.setSlot4Id(characterIds.get(3));
            if (characterIds.size() > 4) party.setSlot5Id(characterIds.get(4));
        }
    }

    @Transactional
    public void dispatchParty(String displayName, UUID questId, List<UUID> characterIds) {
        if (characterIds == null || characterIds.isEmpty()) throw new IllegalArgumentException("Cannot dispatch an empty party.");
        PlayerProfile profile = profileRepo.findByDisplayName(displayName).orElseThrow(() -> new ResourceNotFoundException("Profile not found."));
        QuestTemplate quest = questRepo.findById(questId).orElseThrow(() -> new ResourceNotFoundException("Quest not found."));

        AcceptedMission accepted = acceptedRepo.findByProfileIdAndQuestTemplateId(profile.getId(), questId)
                .orElseThrow(() -> new IllegalStateException("You must accept this mission into your Journal first."));

        List<PlayerCharacter> characters = charRepo.findAllById(characterIds);
        for (PlayerCharacter pc : characters) {
            if (!"IDLE".equalsIgnoreCase(pc.getStatus())) {
                throw new IllegalStateException("Character " + pc.getTemplate().getName() + " is not IDLE.");
            }
            pc.setStatus("MISSION");
        }
        charRepo.saveAll(characters);

        Party expeditionParty = new Party();
        expeditionParty.setOwner(profile);
        assignPartySlots(expeditionParty, characterIds);
        partyRepo.save(expeditionParty);

        ActiveMission activeMission = new ActiveMission();
        activeMission.setParty(expeditionParty);
        activeMission.setQuestTemplate(quest);
        activeMission.setSuccessChance(calculateSuccessChance(characterIds, questId));
        activeMission.setDispatchTime(Instant.now());
        activeMission.setEndTime(Instant.now().plus(quest.getDurationHours() != null ? quest.getDurationHours() : 4, ChronoUnit.HOURS));

        activeMissionRepo.save(activeMission);
        acceptedRepo.delete(accepted);
    }

    @Transactional
    public List<ActiveMissionResponse> getActiveMissions(String displayName) {
        PlayerProfile profile = profileRepo.findByDisplayName(displayName).orElseThrow(() -> new ResourceNotFoundException("Profile not found."));
        resolveCompletedMissions(profile);
        return activeMissionRepo.findByPartyOwnerId(profile.getId()).stream().map(mission -> {
            QuestTemplate qt = mission.getQuestTemplate();
            List<PlayerCharacter> partyMembers = getPartyMembers(mission.getParty());
            List<ActiveMissionResponse.CharacterSnippet> snippets = partyMembers.stream()
                    .map(pc -> ActiveMissionResponse.CharacterSnippet.builder()
                            .characterId(pc.getId())
                            .name(pc.getTemplate().getName())
                            .portraitUrl(pc.getTemplate().getPortraitUrl())
                            .flavorQuipsJson(pc.getTemplate().getFlavorQuips())
                            .build()).collect(Collectors.toList());

            return ActiveMissionResponse.builder()
                    .missionId(mission.getId())
                    .questTitle(qt.getTitle())
                    .questType(qt.getType().name())
                    .successChance(mission.getSuccessChance())
                    .dispatchTimeEpoch(mission.getDispatchTime().toEpochMilli())
                    .endTimeEpoch(mission.getEndTime().toEpochMilli())
                    .isResolved(mission.isResolved())
                    .wasSuccessful(mission.isWasSuccessful())
                    // Map rewards directly into the response so UI can show them without lookup
                    .rewardGold(qt.getRewardGold() != null ? qt.getRewardGold() : 0)
                    .rewardGems(qt.getRewardGems() != null ? qt.getRewardGems() : 0)
                    .rewardFood(qt.getRewardFood() != null ? qt.getRewardFood() : 0)
                    .rewardWood(qt.getRewardWood() != null ? qt.getRewardWood() : 0)
                    .rewardStone(qt.getRewardStone() != null ? qt.getRewardStone() : 0)
                    .rewardExp(qt.getBaseExp() != null ? qt.getBaseExp() : 0)
                    .partyMembers(snippets)
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional
    public void resolveCompletedMissions(PlayerProfile profile) {
        List<ActiveMission> missions = activeMissionRepo.findByPartyOwnerId(profile.getId());
        Instant now = Instant.now();

        for (ActiveMission mission : missions) {
            if (now.isBefore(mission.getEndTime()) || mission.isResolved()) continue;

            boolean success = (random.nextInt(100) + 1) <= mission.getSuccessChance();
            mission.setResolved(true);
            mission.setWasSuccessful(success);
            activeMissionRepo.save(mission);
        }
    }

    @Transactional
    public void claimMission(UUID missionId, String displayName) {
        PlayerProfile profile = profileRepo.findByDisplayName(displayName)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found."));
        ActiveMission mission = activeMissionRepo.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found."));

        if (!mission.getParty().getOwner().getId().equals(profile.getId())) {
            throw new IllegalStateException("You do not own this mission.");
        }
        if (!mission.isResolved()) {
            throw new IllegalStateException("This mission has not finished yet.");
        }

        QuestTemplate quest = mission.getQuestTemplate();
        List<PlayerCharacter> characters = getPartyMembers(mission.getParty());

        if (mission.isWasSuccessful()) {
            PlayerResources resources = profile.getResources();
            resources.setGold(resources.getGold() + (quest.getRewardGold() != null ? quest.getRewardGold() : 0));
            resources.setGems(resources.getGems() + (quest.getRewardGems() != null ? quest.getRewardGems() : 0));
            resources.setFood(resources.getFood() + (quest.getRewardFood() != null ? quest.getRewardFood() : 0));
            resources.setWood(resources.getWood() + (quest.getRewardWood() != null ? quest.getRewardWood() : 0));
            resources.setStone(resources.getStone() + (quest.getRewardStone() != null ? quest.getRewardStone() : 0));

            int xpShare = (quest.getBaseExp() != null && !characters.isEmpty()) ? (quest.getBaseExp() / characters.size()) : 0;

            for (PlayerCharacter pc : characters) {
                pc.setCurrentXp(pc.getCurrentXp() + xpShare);
                processLevelUp(pc);
                pc.setCurrentHp(Math.max(1, pc.getCurrentHp() - Math.max(1, (int) (pc.getMaxHp() * 0.10))));
                pc.setStatus("IDLE");
            }

            CompletedMission cm = new CompletedMission();
            cm.setProfile(profile);
            cm.setQuestTemplate(quest);
            completedRepo.save(cm);
        } else {
            for (PlayerCharacter pc : characters) {
                pc.setCurrentHp(Math.max(1, pc.getCurrentHp() - Math.max(1, (int) (pc.getMaxHp() * 0.90))));
                pc.setStatus(pc.getCurrentHp() == 1 ? "KO" : "IDLE");
            }

            AcceptedMission am = new AcceptedMission();
            am.setProfile(profile);
            am.setQuestTemplate(quest);
            acceptedRepo.save(am);
        }

        charRepo.saveAll(characters);
        profileRepo.save(profile);

        Party party = mission.getParty();
        activeMissionRepo.delete(mission);
        partyRepo.delete(party);
    }

    private void processLevelUp(PlayerCharacter pc) {
        int newLevel = DndMathUtility.calculateLevelFromXp(pc.getCurrentXp());
        if (newLevel > pc.getCurrentLevel()) {
            int levelDiff = newLevel - pc.getCurrentLevel();
            int hpGain = Math.max(1, ((pc.getTemplate().getHitDieType() / 2) + 1) + DndMathUtility.calculateModifier(pc.getTotalConstitution())) * levelDiff;
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