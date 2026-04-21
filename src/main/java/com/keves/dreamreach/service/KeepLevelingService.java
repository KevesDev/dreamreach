package com.keves.dreamreach.service;

import com.keves.dreamreach.config.GameKeepConfig;
import com.keves.dreamreach.config.GameEconomyConfig;
import com.keves.dreamreach.entity.BuildingInstance;
import com.keves.dreamreach.entity.PlayerCharacter;
import com.keves.dreamreach.entity.PlayerProfile;
import com.keves.dreamreach.entity.PlayerResources;
import com.keves.dreamreach.entity.UpgradeTask;
import com.keves.dreamreach.repository.PlayerProfileRepository;
import com.keves.dreamreach.repository.UpgradeTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class KeepLevelingService {

    private final GameKeepConfig keepConfig;
    private final GameEconomyConfig economyConfig;
    private final PlayerProfileRepository profileRepository;
    private final UpgradeTaskRepository upgradeTaskRepository;
    private final EconomyService economyService;

    public KeepLevelingService(GameKeepConfig keepConfig, GameEconomyConfig economyConfig,
                               PlayerProfileRepository profileRepository, UpgradeTaskRepository upgradeTaskRepository,
                               EconomyService economyService) {
        this.keepConfig = keepConfig;
        this.economyConfig = economyConfig;
        this.profileRepository = profileRepository;
        this.upgradeTaskRepository = upgradeTaskRepository;
        this.economyService = economyService;
    }

    public int getRequiredPopulation(int targetLevel) {
        return (int) (keepConfig.getBasePopulation() * Math.pow(targetLevel, keepConfig.getPopulationExponent()));
    }

    public int getRequiredHeroCount(int targetLevel) {
        return (int) (keepConfig.getBaseHeroCount() * Math.pow(targetLevel, keepConfig.getHeroCountExponent()));
    }

    public int getRequiredHeroLevel(int targetLevel) {
        return (int) (keepConfig.getBaseHeroLevel() * Math.pow(targetLevel, keepConfig.getHeroLevelExponent()));
    }

    public int getClampedResourceCost(int targetLevel, int currentKeepLevel, int baseCost) {
        double theoreticalCost = baseCost * Math.pow(targetLevel, keepConfig.getCostExponent());
        int currentStorageCap = currentKeepLevel * economyConfig.getBaseStoragePerKeepLevel();
        int maxAllowedCost = (int) (currentStorageCap * keepConfig.getMaxCostPercentageOfStorage());
        return (int) Math.min(theoreticalCost, maxAllowedCost);
    }

    public int getWoodCost(int targetLevel, int currentKeepLevel) {
        return getClampedResourceCost(targetLevel, currentKeepLevel, keepConfig.getBaseWoodCost());
    }

    public int getStoneCost(int targetLevel, int currentKeepLevel) {
        return getClampedResourceCost(targetLevel, currentKeepLevel, keepConfig.getBaseStoneCost());
    }

    public int getUpgradeTimeMinutes(int targetLevel) {
        return keepConfig.getBaseKeepUpgradeTimeMinutes() * targetLevel;
    }

    @Transactional
    public void startKeepUpgrade(PlayerProfile profile) {
        economyService.updateProductionState(profile);

        BuildingInstance keep = profile.getBuildings().stream()
                .filter(b -> b.getBuildingType().equalsIgnoreCase("keep"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Keep not found."));

        int targetLevel = keep.getLevel() + 1;

        if (upgradeTaskRepository.findByBuildingInstanceId(keep.getId()).isPresent()) {
            throw new IllegalStateException("The Keep is already being upgraded.");
        }

        // --- VALIDATE POPULATION ---
        int currentPop = profile.getPopulation() != null ? profile.getPopulation().getTotalPopulation() : 0;
        if (currentPop < getRequiredPopulation(targetLevel)) {
            throw new IllegalStateException("Kingdom Population requirement not met.");
        }

        // --- VALIDATE HERO ROSTER ---
        int reqHeroLevel = getRequiredHeroLevel(targetLevel);
        long validHeroCount = profile.getCharacters().stream()
                .filter(c -> c.getCurrentLevel() >= reqHeroLevel)
                .count();
        if (validHeroCount < getRequiredHeroCount(targetLevel)) {
            throw new IllegalStateException("Hero Roster requirement not met.");
        }

        // --- VALIDATE RESOURCES ---
        int reqWood = getWoodCost(targetLevel, keep.getLevel());
        int reqStone = getStoneCost(targetLevel, keep.getLevel());
        PlayerResources res = profile.getResources();

        if (res.getWood() < reqWood || res.getStone() < reqStone) {
            throw new IllegalStateException("Insufficient resources in the Treasury.");
        }

        res.setWood(res.getWood() - reqWood);
        res.setStone(res.getStone() - reqStone);

        // --- START EXPONENTIAL/LINEAR TIMER ---
        Instant now = Instant.now();
        Instant completion = now.plus(getUpgradeTimeMinutes(targetLevel), ChronoUnit.MINUTES);

        UpgradeTask task = UpgradeTask.builder()
                .profile(profile)
                .buildingInstance(keep)
                .targetLevel(targetLevel)
                .startTime(now)
                .completionTime(completion)
                .build();

        upgradeTaskRepository.save(task);
        profileRepository.save(profile);
    }
}