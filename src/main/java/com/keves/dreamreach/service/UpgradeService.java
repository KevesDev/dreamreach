package com.keves.dreamreach.service;

import com.keves.dreamreach.config.GameEconomyConfig;
import com.keves.dreamreach.config.GameLedgerConfig;
import com.keves.dreamreach.entity.BuildingInstance;
import com.keves.dreamreach.entity.PlayerProfile;
import com.keves.dreamreach.entity.PlayerResources;
import com.keves.dreamreach.entity.UpgradeTask;
import com.keves.dreamreach.repository.BuildingInstanceRepository;
import com.keves.dreamreach.repository.PlayerProfileRepository;
import com.keves.dreamreach.repository.UpgradeTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class UpgradeService {

    private final UpgradeTaskRepository taskRepository;
    private final BuildingInstanceRepository buildingRepository;
    private final PlayerProfileRepository profileRepository;
    private final EconomyService economyService;
    private final GameEconomyConfig economyConfig;
    private final LedgerService ledgerService;
    private final GameLedgerConfig ledgerConfig;

    public UpgradeService(UpgradeTaskRepository taskRepository, BuildingInstanceRepository buildingRepository,
                          PlayerProfileRepository profileRepository, EconomyService economyService,
                          GameEconomyConfig economyConfig, LedgerService ledgerService, GameLedgerConfig ledgerConfig) {
        this.taskRepository = taskRepository;
        this.buildingRepository = buildingRepository;
        this.profileRepository = profileRepository;
        this.economyService = economyService;
        this.economyConfig = economyConfig;
        this.ledgerService = ledgerService;
        this.ledgerConfig = ledgerConfig;
    }

    public int getUpgradeWoodCost(String buildingType, int targetLevel) {
        int baseCost = switch (buildingType.toLowerCase()) {
            case "house" -> economyConfig.getCostHouseWood();
            case "bakery" -> economyConfig.getCostBakeryWood();
            case "lodge" -> economyConfig.getCostLodgeWood();
            case "tower" -> economyConfig.getCostTowerWood();
            case "tavern" -> economyConfig.getCostTavernWood();
            default -> 0;
        };
        return (int) (baseCost * Math.pow(targetLevel, 1.5));
    }

    public int getUpgradeStoneCost(String buildingType, int targetLevel) {
        int baseCost = switch (buildingType.toLowerCase()) {
            case "house" -> economyConfig.getCostHouseStone();
            case "bakery" -> economyConfig.getCostBakeryStone();
            case "lodge" -> economyConfig.getCostLodgeStone();
            case "tower" -> economyConfig.getCostTowerStone();
            case "tavern" -> economyConfig.getCostTavernStone();
            default -> 0;
        };
        return (int) (baseCost * Math.pow(targetLevel, 1.5));
    }

    public int getUpgradeTimeSeconds(String buildingType, int targetLevel) {
        int baseTime = switch (buildingType.toLowerCase()) {
            case "house" -> economyConfig.getBuildTimeHouse();
            case "bakery" -> economyConfig.getBuildTimeBakery();
            case "lodge" -> economyConfig.getBuildTimeLodge();
            case "tower" -> economyConfig.getBuildTimeTower();
            case "tavern" -> economyConfig.getBuildTimeTavern();
            default -> 60;
        };
        return (int) (baseTime * Math.pow(targetLevel, economyConfig.getBuildingUpgradeTimeExponent()));
    }

    @Transactional
    public void startUpgrade(PlayerProfile profile, UUID buildingInstanceId) {
        economyService.updateProductionState(profile);

        BuildingInstance building = buildingRepository.findById(buildingInstanceId)
                .orElseThrow(() -> new IllegalArgumentException("Building not found."));

        if (!building.getProfile().getId().equals(profile.getId())) {
            throw new SecurityException("You do not own this building.");
        }

        if (building.getBuildingType().equalsIgnoreCase("keep")) {
            throw new IllegalArgumentException("Keep upgrades must be routed through the KeepLevelingService.");
        }

        int targetLevel = building.getLevel() + 1;

        int currentKeepLevel = profile.getBuildings().stream()
                .filter(b -> b.getBuildingType().equalsIgnoreCase("keep"))
                .mapToInt(BuildingInstance::getLevel)
                .max().orElse(1);

        if (targetLevel > currentKeepLevel) {
            throw new IllegalStateException("Building level cannot exceed current Keep level.");
        }

        if (taskRepository.findByBuildingInstanceId(buildingInstanceId).isPresent()) {
            throw new IllegalStateException("This building is already being upgraded.");
        }

        int reqWood = getUpgradeWoodCost(building.getBuildingType(), targetLevel);
        int reqStone = getUpgradeStoneCost(building.getBuildingType(), targetLevel);
        PlayerResources res = profile.getResources();

        if (res.getWood() < reqWood || res.getStone() < reqStone) {
            throw new IllegalStateException("Not enough resources to upgrade.");
        }

        res.setWood(res.getWood() - reqWood);
        res.setStone(res.getStone() - reqStone);

        Instant now = Instant.now();
        Instant completion = now.plus(getUpgradeTimeSeconds(building.getBuildingType(), targetLevel), ChronoUnit.SECONDS);

        UpgradeTask task = UpgradeTask.builder()
                .profile(profile)
                .buildingInstance(building)
                .targetLevel(targetLevel)
                .startTime(now)
                .completionTime(completion)
                .build();

        taskRepository.save(task);
        profileRepository.save(profile);
    }

    @Transactional
    public void completeUpgrade(PlayerProfile profile, UUID buildingInstanceId) {
        UpgradeTask task = taskRepository.findByBuildingInstanceId(buildingInstanceId)
                .orElseThrow(() -> new IllegalStateException("No active upgrade task found for this building."));

        if (!task.getProfile().getId().equals(profile.getId())) {
            throw new SecurityException("You do not own this upgrade task.");
        }

        if (Instant.now().isBefore(task.getCompletionTime())) {
            throw new IllegalStateException("The upgrade is not yet complete.");
        }

        BuildingInstance building = task.getBuildingInstance();
        building.setLevel(task.getTargetLevel());

        taskRepository.delete(task);
        buildingRepository.save(building);

        // Ledger Hook for Civic Progress
        String msg = building.getBuildingType().equalsIgnoreCase("keep")
                ? "A grand celebration marks the expansion of the Keep to Level " + building.getLevel() + "."
                : "The " + building.getBuildingType() + " has been upgraded to Level " + building.getLevel() + ".";

        ledgerService.appendLog(profile, "CIVIC", msg);
    }
}