package com.keves.dreamreach.service;

import com.keves.dreamreach.config.GameEconomyConfig;
import com.keves.dreamreach.entity.PlayerPopulation;
import com.keves.dreamreach.entity.PlayerProfile;
import com.keves.dreamreach.entity.PlayerResources;
import com.keves.dreamreach.repository.PlayerProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * The core engine for the Dreamreach macro-economy.
 * Handles the 'State-Based' resource accrual to prevent 'half-cycle' loss
 * when worker assignments change.
 */
@Service
public class EconomyService {

    private final GameEconomyConfig economyConfig;
    private final PlayerProfileRepository profileRepository;

    public EconomyService(GameEconomyConfig economyConfig, PlayerProfileRepository profileRepository) {
        this.economyConfig = economyConfig;
        this.profileRepository = profileRepository;
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
                .mapToInt(b -> b.getAssignedWorkers() * economyConfig.getFoodPerBakery())
                .sum();

        int production = (pop.getHunters() * economyConfig.getFoodPerHunter()) + bakeryProduction;

        int consumption = pop.getTotalPopulation() * economyConfig.getFoodConsumedPerPeasant();

        return production - consumption;
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

        // Apply happiness modifier based on active tax bracket
        int happinessMod = switch (profile.getTaxBracket().toUpperCase()) {
            case "LOW" -> economyConfig.getHappinessModifierLowTax();
            case "HIGH" -> economyConfig.getHappinessModifierHighTax();
            default -> economyConfig.getHappinessModifierNormalTax();
        };

        // Calculate accrued happiness over the timeframe elapsed
        int totalHappinessChange = (int) (happinessMod * hoursElapsed);
        int newHappiness = profile.getHappiness() + totalHappinessChange;

        // Clamp happiness firmly between 0 and the established maximum cap
        if (newHappiness > economyConfig.getMaxHappiness()) newHappiness = economyConfig.getMaxHappiness();
        if (newHappiness < 0) newHappiness = 0;
        profile.setHappiness(newHappiness);

        int newPendingFood = res.getPendingFood() + (int)(foodRate * hoursElapsed);
        int newPendingWood = res.getPendingWood() + (int)(woodRate * hoursElapsed);
        int newPendingStone = res.getPendingStone() + (int)(stoneRate * hoursElapsed);
        int newPendingGold = res.getPendingGold() + (int)(goldRate * hoursElapsed);

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
            int currentHappiness = profile.getHappiness();

            int houseCount = (int) profile.getBuildings().stream()
                    .filter(b -> b.getBuildingType().equalsIgnoreCase("house")).count();
            int maxPop = houseCount * economyConfig.getCapacityPerHouse();

            // Run the RNG evaluation exactly as many times as the 15-minute interval passed
            for (int i = 0; i < ticksPassed; i++) {
                int currentPop = pop.getTotalPopulation();

                // Happiness scales the likelihood of joining vs leaving
                double joinWeightMod = (currentHappiness - 50) * economyConfig.getHappinessImpactMultiplier();
                double effectiveJoinWeight = Math.max(0.0, Math.min(1.0, economyConfig.getBaseJoinWeight() + joinWeightMod));

                // Extreme happiness (very high or very low) increases overall activity
                double movementMod = Math.abs(currentHappiness - 50) * (economyConfig.getHappinessImpactMultiplier() / 2.0);
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

        res.setFood(Math.max(0, res.getFood() + res.getPendingFood()));
        res.setWood(Math.max(0, res.getWood() + res.getPendingWood()));
        res.setStone(Math.max(0, res.getStone() + res.getPendingStone()));

        // Note: Gold is explicitly NOT claimed here anymore. It has a separate 1-hour collection cycle via collectTaxes().

        res.setPendingFood(0);
        res.setPendingWood(0);
        res.setPendingStone(0);

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
    }

    /**
     * Validates the 1-hour cooldown and moves pending gold into the spendable treasury.
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
        res.setGold(Math.max(0, res.getGold() + res.getPendingGold()));
        res.setPendingGold(0);

        profile.setLastTaxCollectionTime(now);
        profileRepository.save(profile);
    }
}