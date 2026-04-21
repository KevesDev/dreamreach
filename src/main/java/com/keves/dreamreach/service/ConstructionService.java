package com.keves.dreamreach.service;

import com.keves.dreamreach.config.GameEconomyConfig;
import com.keves.dreamreach.config.GameLedgerConfig;
import com.keves.dreamreach.entity.BuildingInstance;
import com.keves.dreamreach.entity.ConstructionTask;
import com.keves.dreamreach.entity.PlayerProfile;
import com.keves.dreamreach.entity.PlayerResources;
import com.keves.dreamreach.repository.ConstructionTaskRepository;
import com.keves.dreamreach.repository.PlayerProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class ConstructionService {

    private final ConstructionTaskRepository taskRepository;
    private final PlayerProfileRepository profileRepository;
    private final EconomyService economyService;
    private final GameEconomyConfig config;
    private final LedgerService ledgerService;
    private final GameLedgerConfig ledgerConfig;

    public ConstructionService(ConstructionTaskRepository taskRepository,
                               PlayerProfileRepository profileRepository,
                               EconomyService economyService,
                               GameEconomyConfig config,
                               LedgerService ledgerService,
                               GameLedgerConfig ledgerConfig) {
        this.taskRepository = taskRepository;
        this.profileRepository = profileRepository;
        this.economyService = economyService;
        this.config = config;
        this.ledgerService = ledgerService;
        this.ledgerConfig = ledgerConfig;
    }

    @Transactional
    public void startConstruction(PlayerProfile profile, String buildingType) {
        economyService.updateProductionState(profile);

        if (taskRepository.findByProfileIdAndBuildingType(profile.getId(), buildingType.toLowerCase()).isPresent()) {
            throw new IllegalStateException("You are already constructing a " + buildingType + ".");
        }

        int currentKeepLevel = profile.getBuildings().stream()
                .filter(b -> b.getBuildingType().equalsIgnoreCase("keep"))
                .mapToInt(BuildingInstance::getLevel)
                .max().orElse(1);

        int costWood = 0;
        int costStone = 0;
        int buildTimeSeconds = 0;

        switch (buildingType.toLowerCase()) {
            case "house":
                costWood = config.getCostHouseWood();
                costStone = config.getCostHouseStone();
                buildTimeSeconds = config.getBuildTimeHouse();
                break;
            case "bakery":
                costWood = config.getCostBakeryWood();
                costStone = config.getCostBakeryStone();
                buildTimeSeconds = config.getBuildTimeBakery();
                break;
            case "tower":
                if (currentKeepLevel < config.getTowerUnlockLevel()) {
                    throw new IllegalStateException("Requires Keep Level " + config.getTowerUnlockLevel());
                }
                costWood = config.getCostTowerWood();
                costStone = config.getCostTowerStone();
                buildTimeSeconds = config.getBuildTimeTower();
                break;
            case "lodge":
                costWood = config.getCostLodgeWood();
                costStone = config.getCostLodgeStone();
                buildTimeSeconds = config.getBuildTimeLodge();
                break;
            case "tavern":
                if (currentKeepLevel < config.getTavernUnlockLevel()) {
                    throw new IllegalStateException("Requires Keep Level " + config.getTavernUnlockLevel());
                }
                if (profile.getBuildings().stream().anyMatch(b -> b.getBuildingType().equalsIgnoreCase("tavern"))) {
                    throw new IllegalStateException("You can only have one Tavern in your kingdom.");
                }
                costWood = config.getCostTavernWood();
                costStone = config.getCostTavernStone();
                buildTimeSeconds = config.getBuildTimeTavern();
                break;
            default:
                throw new IllegalArgumentException("Unknown building type: " + buildingType);
        }

        PlayerResources res = profile.getResources();
        if (res.getWood() < costWood || res.getStone() < costStone) {
            throw new IllegalStateException("Not enough resources to construct " + buildingType + ".");
        }

        res.setWood(res.getWood() - costWood);
        res.setStone(res.getStone() - costStone);

        Instant now = Instant.now();
        Instant completion = now.plus(buildTimeSeconds, ChronoUnit.SECONDS);

        ConstructionTask newTask = ConstructionTask.builder()
                .profile(profile)
                .buildingType(buildingType.toLowerCase())
                .targetLevel(1)
                .startTime(now)
                .completionTime(completion)
                .build();

        taskRepository.save(newTask);
        profileRepository.save(profile);
    }

    @Transactional
    public void completeConstruction(PlayerProfile profile, String buildingType) {
        ConstructionTask task = taskRepository.findByProfileIdAndBuildingType(profile.getId(), buildingType.toLowerCase())
                .orElseThrow(() -> new IllegalStateException("No active construction found for " + buildingType));

        if (Instant.now().isBefore(task.getCompletionTime())) {
            throw new IllegalStateException("The construction of " + buildingType + " is not yet complete.");
        }

        BuildingInstance newBuilding = new BuildingInstance();
        newBuilding.setBuildingType(buildingType.toLowerCase());
        newBuilding.setLevel(1);
        newBuilding.setAssignedWorkers(0);
        newBuilding.setProfile(profile);

        profile.getBuildings().add(newBuilding);

        taskRepository.delete(task);
        profileRepository.save(profile);

        String msg = ledgerConfig.getConstructionCompleteMessage()
                .replace("{buildingType}", buildingType.toLowerCase());
        ledgerService.appendLog(profile, "CIVIC", msg);
    }
}