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
        if (res.getFood() + newPendingFood < 0) {
            newPendingFood = -res.getFood();
        }
        if (res.getWood() + newPendingWood < 0) {
            newPendingWood = -res.getWood();
        }
        if (res.getStone() + newPendingStone < 0) {
            newPendingStone = -res.getStone();
        }
        if (res.getGold() + newPendingGold < 0) {
            newPendingGold = -res.getGold();
        }

        res.setPendingFood(newPendingFood);
        res.setPendingWood(newPendingWood);
        res.setPendingStone(newPendingStone);
        res.setPendingGold(newPendingGold);

        res.setLastUpdate(now);

        profileRepository.save(profile);
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