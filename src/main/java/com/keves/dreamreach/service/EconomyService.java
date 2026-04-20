package com.keves.dreamreach.service;

import com.keves.dreamreach.config.GameEconomyConfig;
import com.keves.dreamreach.config.GameLedgerConfig;
import com.keves.dreamreach.entity.BuildingInstance;
import com.keves.dreamreach.entity.PlayerPopulation;
import com.keves.dreamreach.entity.PlayerProfile;
import com.keves.dreamreach.entity.PlayerResources;
import com.keves.dreamreach.repository.BuildingInstanceRepository;
import com.keves.dreamreach.repository.PlayerProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * The core engine for the Dreamreach macro-economy.
 * Handles the 'State-Based' resource accrual to prevent 'half-cycle' loss
 * when worker assignments change.
 */
@Service
public class EconomyService {

    private final GameEconomyConfig economyConfig;
    private final PlayerProfileRepository profileRepository;
    private final BuildingInstanceRepository buildingRepository;
    private final LedgerService ledgerService;
    private final GameLedgerConfig ledgerConfig;

    public EconomyService(GameEconomyConfig economyConfig,
                          PlayerProfileRepository profileRepository,
                          BuildingInstanceRepository buildingRepository,
                          LedgerService ledgerService,
                          GameLedgerConfig ledgerConfig) {
        this.economyConfig = economyConfig;
        this.profileRepository = profileRepository;
        this.buildingRepository = buildingRepository;
        this.ledgerService = ledgerService;
        this.ledgerConfig = ledgerConfig;
    }

    /**
     * Calculates the net Food production per hour.
     */
    public int calculateFoodRate(PlayerProfile profile) {
        PlayerPopulation pop = profile.getPopulation();
        if (pop == null) return 0;

        // Iterate through physical bakeries and multiply their specific assigned workers by the base rate
        int bakeryProduction = profile.getBuildings().stream()
                .filter(b -> b.getBuildingType().equalsIgnoreCase("bakery"))
                .mapToInt(b -> b.getAssignedWorkers() * economyConfig.getFoodPerBaker())
                .sum();

        // Iterate through physical lodges and multiply their specific assigned workers by the base rate
        int lodgeProduction = profile.getBuildings().stream()
                .filter(b -> b.getBuildingType().equalsIgnoreCase("lodge"))
                .mapToInt(b -> b.getAssignedWorkers() * economyConfig.getFoodPerHunter())
                .sum();

        int production = lodgeProduction + bakeryProduction;

        int consumption = pop.getTotalPopulation() * economyConfig.getFoodConsumedPerPeasant();

        return production - consumption;
    }

    /**
     * Increments the assigned workers for a specific building instance.
     * Validates against professional population and building capacity.
     */
    @Transactional
    public void assignWorker(PlayerProfile profile, UUID buildingId) {
        updateProductionState(profile); // Ensure state is flushed before rates change

        BuildingInstance building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new IllegalArgumentException("Building not found with ID: " + buildingId));

        if (!building.getProfile().getId().equals(profile.getId())) {
            throw new IllegalArgumentException("Unauthorized: This building does not belong to the active profile.");
        }

        String type = building.getBuildingType().toLowerCase();
        int maxCap = type.equals("bakery") ? economyConfig.getMaxWorkersBakery() : economyConfig.getMaxWorkersLodge();

        if (building.getAssignedWorkers() >= maxCap) {
            throw new IllegalStateException("This structure is already at maximum worker capacity.");
        }

        // Logic to verify there are unassigned professionals available in the population
        PlayerPopulation pop = profile.getPopulation();
        int currentlyAssignedTotal = profile.getBuildings().stream()
                .filter(b -> b.getBuildingType().equalsIgnoreCase(building.getBuildingType()))
                .mapToInt(BuildingInstance::getAssignedWorkers)
                .sum();

        int totalTrained = type.equals("bakery") ? pop.getBakers() : pop.getHunters();

        if (currentlyAssignedTotal >= totalTrained) {
            throw new IllegalStateException("No available " + (type.equals("bakery") ? "Bakers" : "Hunters") + " are idle to assign.");
        }

        building.setAssignedWorkers(building.getAssignedWorkers() + 1);
        buildingRepository.save(building);
    }

    /**
     * Decrements the assigned workers for a specific building instance.
     */
    @Transactional
    public void removeWorker(PlayerProfile profile, UUID buildingId) {
        updateProductionState(profile);

        BuildingInstance building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new IllegalArgumentException("Building not found with ID: " + buildingId));

        if (building.getAssignedWorkers() <= 0) {
            throw new IllegalStateException("There are no workers currently assigned to this structure.");
        }

        building.setAssignedWorkers(building.getAssignedWorkers() - 1);
        buildingRepository.save(building);
    }

    /**
     * Core dynamic evaluator for the kingdom's target happiness score.
     */
    private int calculateTargetHappiness(PlayerProfile profile) {
        int target = economyConfig.getTargetHappinessBase();
        PlayerResources res = profile.getResources();
        PlayerPopulation pop = profile.getPopulation();

        // 1. Tax Policy Evaluation
        switch (profile.getTaxBracket().toUpperCase()) {
            case "LOW" -> target += economyConfig.getTargetModLowTax();
            case "HIGH" -> target += economyConfig.getTargetModHighTax();
            default -> target += economyConfig.getTargetModNormalTax();
        }

        // 2. Food Scarcity Evaluation
        if (res.getFood() > 0) {
            target += economyConfig.getTargetModHasFood();
        } else {
            target += economyConfig.getTargetModNoFood();
        }

        // 3. Housing Density Evaluation
        int houseCount = (int) profile.getBuildings().stream().filter(b -> b.getBuildingType().equalsIgnoreCase("house")).count();
        int maxPop = houseCount * economyConfig.getCapacityPerHouse();

        if (pop.getTotalPopulation() < maxPop) {
            target += economyConfig.getTargetModAvailableHousing();
        } else {
            target += economyConfig.getTargetModFullHousing();
        }

        // 4. Labor Market Evaluation
        boolean hasAvailableJobs = false;
        boolean hasBuildingsWithJobs = false;
        for (BuildingInstance b : profile.getBuildings()) {
            String type = b.getBuildingType().toLowerCase();
            if (type.equals("bakery") || type.equals("lodge")) {
                hasBuildingsWithJobs = true;
                int maxCap = type.equals("bakery") ? economyConfig.getMaxWorkersBakery() : economyConfig.getMaxWorkersLodge();
                if (b.getAssignedWorkers() < maxCap) {
                    hasAvailableJobs = true;
                    break;
                }
            }
        }

        if (hasBuildingsWithJobs) {
            if (hasAvailableJobs) {
                target += economyConfig.getTargetModAvailableJobs();
            } else {
                target += economyConfig.getTargetModNoJobs();
            }
        }

        // Clamp the theoretical target
        return Math.max(0, Math.min(economyConfig.getMaxHappiness(), target));
    }

    /**
     * STATE-BASED ACCRUAL LOGIC:
     * This method 'flushes' all resources earned at the OLD rate into the 'Pending' pool.
     */
    @Transactional
    public void updateProductionState(PlayerProfile profile) {
        PlayerResources res = profile.getResources();
        PlayerPopulation pop = profile.getPopulation();

        if (res == null || pop == null) return;

        Instant now = Instant.now();
        double hoursElapsed = Duration.between(res.getLastUpdate(), now).toMillis() / 3600000.0;

        int foodRate = calculateFoodRate(profile);
        int woodRate = (pop.getWoodcutters() * economyConfig.getWoodPerWoodcutter()) + economyConfig.getBasePassiveWood();
        int stoneRate = (pop.getStoneworkers() * economyConfig.getStonePerStoneworker()) + economyConfig.getBasePassiveStone();

        // Calculate gold rate based on active tax bracket multiplier
        double taxMultiplier = switch (profile.getTaxBracket().toUpperCase()) {
            case "LOW" -> economyConfig.getTaxRateLowMultiplier();
            case "HIGH" -> economyConfig.getTaxRateHighMultiplier();
            default -> economyConfig.getTaxRateNormalMultiplier();
        };
        int goldRate = (int) (pop.getTotalPopulation() * economyConfig.getTaxGoldPerCitizenPerHour() * taxMultiplier);

        // --- DYNAMIC HAPPINESS LERP CALCULATION ---
        int currentHappiness = profile.getHappiness();
        int targetHappiness = calculateTargetHappiness(profile);
        double maxMovement = hoursElapsed * economyConfig.getHappinessInterpolationRatePerHour();

        int newHappiness = currentHappiness;

        if (currentHappiness < targetHappiness) {
            newHappiness = (int) Math.min(targetHappiness, currentHappiness + maxMovement);
        } else if (currentHappiness > targetHappiness) {
            newHappiness = (int) Math.max(targetHappiness, currentHappiness - maxMovement);
        }

        // Clamp happiness firmly between 0 and the established maximum cap
        if (newHappiness > economyConfig.getMaxHappiness()) newHappiness = economyConfig.getMaxHappiness();
        if (newHappiness < 0) newHappiness = 0;

        // Ledger Event Triggers based on severe structural thresholds
        if (res.getFood() <= 0 && newHappiness < 40 && currentHappiness >= 40) {
            ledgerService.appendLog(profile, "CRISIS", ledgerConfig.getEconomyStarvationMessage());
        }
        if (newHappiness >= 80 && currentHappiness < 80) {
            ledgerService.appendLog(profile, "CIVIC", ledgerConfig.getEconomyUtopiaMessage());
        }

        profile.setHappiness(newHappiness);

        double newPendingFood = res.getPendingFood() + (foodRate * hoursElapsed);
        double newPendingWood = res.getPendingWood() + (woodRate * hoursElapsed);
        double newPendingStone = res.getPendingStone() + (stoneRate * hoursElapsed);
        double newPendingGold = res.getPendingGold() + (goldRate * hoursElapsed);

        // Clamp pending consumption to prevent total treasury from dropping below zero
        if (res.getFood() + newPendingFood < 0) newPendingFood = -res.getFood();
        if (res.getWood() + newPendingWood < 0) newPendingWood = -res.getWood();
        if (res.getStone() + newPendingStone < 0) newPendingStone = -res.getStone();
        if (res.getGold() + newPendingGold < 0) newPendingGold = -res.getGold();

        res.setPendingFood(newPendingFood);
        res.setPendingWood(newPendingWood);
        res.setPendingStone(newPendingStone);
        res.setPendingGold(newPendingGold);
        res.setLastUpdate(now);

        // --- RNG POPULATION SIMULATION LOOP ---
        Instant lastPopTick = pop.getLastPopulationTick();
        if (lastPopTick == null) {
            lastPopTick = now;
            pop.setLastPopulationTick(now);
        }

        long minutesSincePopTick = Duration.between(lastPopTick, now).toMinutes();
        int ticksPassed = (int) (minutesSincePopTick / economyConfig.getPopTickIntervalMinutes());

        if (ticksPassed > 0) {
            // Use the freshly calculated happiness for the migration modifiers
            int tickHappiness = profile.getHappiness();

            int houseCount = (int) profile.getBuildings().stream()
                    .filter(b -> b.getBuildingType().equalsIgnoreCase("house")).count();
            int maxPop = houseCount * economyConfig.getCapacityPerHouse();

            // Run the RNG evaluation exactly as many times as the 15-minute interval passed
            for (int i = 0; i < ticksPassed; i++) {
                int currentPop = pop.getTotalPopulation();

                // Happiness scales the likelihood of joining vs leaving
                double joinWeightMod = (tickHappiness - 50) * economyConfig.getHappinessImpactMultiplier();
                double effectiveJoinWeight = Math.max(0.0, Math.min(1.0, economyConfig.getBaseJoinWeight() + joinWeightMod));

                // Extreme happiness (very high or very low) increases overall activity
                double movementMod = Math.abs(tickHappiness - 50) * (economyConfig.getHappinessImpactMultiplier() / 2.0);
                double effectiveMovementChance = Math.max(0.0, Math.min(1.0, economyConfig.getBaseMovementChance() + movementMod));

                if (Math.random() < effectiveMovementChance) {
                    double pct = economyConfig.getPopChangePercentMin() + Math.random() * (economyConfig.getPopChangePercentMax() - economyConfig.getPopChangePercentMin());
                    int changeAmount = (int) Math.ceil(currentPop * pct);

                    if (Math.random() < effectiveJoinWeight) {
                        // Immigration: Idle peasants arrive up to the housing cap
                        int spaceLeft = maxPop - currentPop;
                        int actuallyJoining = Math.min(changeAmount, spaceLeft);
                        if (actuallyJoining > 0) {
                            pop.setIdlePeasants(pop.getIdlePeasants() + actuallyJoining);
                        }
                    } else {
                        // Emigration: People leave, restricted by the hard soft-lock floor
                        int actuallyLeaving = Math.min(changeAmount, currentPop - economyConfig.getStartingPeasants());
                        if (actuallyLeaving > 0) {
                            processEmigrationCascade(profile, pop, actuallyLeaving);
                        }
                    }
                }
            }

            // Fast forward the tick tracker so we don't double-count next time
            pop.setLastPopulationTick(lastPopTick.plus(Duration.ofMinutes((long) ticksPassed * economyConfig.getPopTickIntervalMinutes())));
        }

        profileRepository.save(profile);
    }

    /**
     * Handles the structured drain of population when conditions are poor.
     * Drains idle peasants completely before randomly picking off trained professionals.
     */
    private void processEmigrationCascade(PlayerProfile profile, PlayerPopulation pop, int amountToLeave) {
        int remainingToLeave = amountToLeave;

        // Step 1: The Idle Meat-Shield
        if (pop.getIdlePeasants() > 0) {
            int idleDrain = Math.min(pop.getIdlePeasants(), remainingToLeave);
            pop.setIdlePeasants(pop.getIdlePeasants() - idleDrain);
            remainingToLeave -= idleDrain;
        }

        // Step 2 & 3: The Professional Drain & Random Walkout
        while (remainingToLeave > 0) {
            java.util.List<String> availableProfessions = new java.util.ArrayList<>();
            if (pop.getBakers() > 0) availableProfessions.add("baker");
            if (pop.getHunters() > 0) availableProfessions.add("hunter");
            if (pop.getWoodcutters() > 0) availableProfessions.add("woodcutter");
            if (pop.getStoneworkers() > 0) availableProfessions.add("stoneworker");

            if (availableProfessions.isEmpty()) {
                break; // Absolute hard stop, no one left to leave
            }

            // Pick a random specialized profession to deduct
            String chosenProfession = availableProfessions.get((int) (Math.random() * availableProfessions.size()));

            switch (chosenProfession) {
                case "baker": pop.setBakers(pop.getBakers() - 1); break;
                case "hunter": pop.setHunters(pop.getHunters() - 1); break;
                case "woodcutter": pop.setWoodcutters(pop.getWoodcutters() - 1); break;
                case "stoneworker": pop.setStoneworkers(pop.getStoneworkers() - 1); break;
            }

            // Phantom Worker Fix: Remove 1 assigned worker from a physical building instance to keep the economy accurate
            String buildingType = getBuildingTypeForProfession(chosenProfession);
            if (buildingType != null) {
                for (com.keves.dreamreach.entity.BuildingInstance b : profile.getBuildings()) {
                    if (b.getBuildingType().equalsIgnoreCase(buildingType) && b.getAssignedWorkers() > 0) {
                        b.setAssignedWorkers(b.getAssignedWorkers() - 1);
                        break;
                    }
                }
            }

            remainingToLeave--;
        }
    }

    private String getBuildingTypeForProfession(String profession) {
        if ("baker".equalsIgnoreCase(profession)) return "bakery";
        if ("hunter".equalsIgnoreCase(profession)) return "lodge";
        return null;
    }

    /**
     * Moves all resources from the Pending pool into the Main Treasury (Official Balance).
     * This is the logic behind the 'Collect All' button in the UI.
     */
    @Transactional
    public void claimResources(PlayerProfile profile) {
        updateProductionState(profile);

        PlayerResources res = profile.getResources();

        int keepLevel = profile.getBuildings().stream()
                .filter(b -> b.getBuildingType().equalsIgnoreCase("keep"))
                .mapToInt(BuildingInstance::getLevel)
                .max().orElse(1);
        int maxStorage = keepLevel * economyConfig.getBaseStoragePerKeepLevel();

        int claimFood = (int) res.getPendingFood();
        int claimWood = (int) res.getPendingWood();
        int claimStone = (int) res.getPendingStone();

        int actualFoodToAdd = claimFood > 0 ? Math.min(claimFood, Math.max(0, maxStorage - res.getFood())) : claimFood;
        int actualWoodToAdd = claimWood > 0 ? Math.min(claimWood, Math.max(0, maxStorage - res.getWood())) : claimWood;
        int actualStoneToAdd = claimStone > 0 ? Math.min(claimStone, Math.max(0, maxStorage - res.getStone())) : claimStone;

        res.setFood(Math.max(0, res.getFood() + actualFoodToAdd));
        res.setWood(Math.max(0, res.getWood() + actualWoodToAdd));
        res.setStone(Math.max(0, res.getStone() + actualStoneToAdd));

        // Deduct only the exact integer amount we claimed from the pending pool, leaving any fractional remainders intact
        res.setPendingFood(res.getPendingFood() - claimFood);
        res.setPendingWood(res.getPendingWood() - claimWood);
        res.setPendingStone(res.getPendingStone() - claimStone);

        profileRepository.save(profile);
    }

    /**
     * Instantly flushes the economy state and updates the active tax bracket.
     * Prevents players from exploiting the 1-hour collection cycle.
     */
    @Transactional
    public void setTaxBracket(PlayerProfile profile, String bracket) {
        updateProductionState(profile);

        String upper = bracket.toUpperCase();
        if (!upper.equals("LOW") && !upper.equals("NORMAL") && !upper.equals("HIGH")) {
            throw new IllegalArgumentException("Invalid tax bracket.");
        }
        profile.setTaxBracket(upper);
        profileRepository.save(profile);

        // Ledger Event for Tax Change using dynamic config
        String message = ledgerConfig.getTaxChangeMessage()
                .replace("{bracket}", upper);
        ledgerService.appendLog(profile, "CIVIC", message);
    }

    /**
     * Validates the 1-hour cooldown and moves pending gold into the spendable treasury.
     * Note: Gold accrual explicitly bypasses storage capacity caps.
     */
    @Transactional
    public void collectTaxes(PlayerProfile profile) {
        updateProductionState(profile);

        Instant now = Instant.now();
        double minutesElapsed = Duration.between(profile.getLastTaxCollectionTime(), now).toMinutes();

        if (minutesElapsed < economyConfig.getTaxCooldownMinutes()) {
            throw new IllegalStateException("Tax collection is on cooldown.");
        }

        PlayerResources res = profile.getResources();

        int claimGold = (int) res.getPendingGold();

        res.setGold(Math.max(0, res.getGold() + claimGold));
        res.setPendingGold(res.getPendingGold() - claimGold);

        profile.setLastTaxCollectionTime(now);
        profileRepository.save(profile);
    }
}