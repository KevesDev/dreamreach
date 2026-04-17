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
     * Formula: (Hunters + Bakeries) - (Total Population Consumption)
     */
    public int calculateFoodRate(PlayerProfile profile) {
        PlayerPopulation pop = profile.getPopulation();
        if (pop == null) return 0;

        int production = (pop.getHunters() * economyConfig.getFoodPerHunter())
                + (profile.getStructures().getBakeries() * economyConfig.getFoodPerBakery());

        int consumption = pop.getTotalPopulation() * economyConfig.getFoodConsumedPerPeasant();

        return production - consumption;
    }

    /**
     * STATE-BASED ACCRUAL LOGIC:
     * This method 'flushes' all resources earned at the OLD rate into the 'Pending' pool.
     * *
     * * Why is this important?
     * If a user has 1 Woodcutter for 30 mins, then adds 5 more Woodcutters,
     * we must calculate the first 30 mins at the rate of '1' BEFORE changing the rate to '6'.
     */
    @Transactional
    public void updateProductionState(PlayerProfile profile) {
        PlayerResources res = profile.getResources();
        PlayerPopulation pop = profile.getPopulation();

        if (res == null || pop == null) return;

        Instant now = Instant.now();
        // Calculate the exact fraction of an hour that has passed since the last state change
        double hoursElapsed = Duration.between(res.getLastUpdate(), now).toMillis() / 3600000.0;

        // 1. Calculate the rates based on the state AT THIS MOMENT
        int foodRate = calculateFoodRate(profile);
        int woodRate = pop.getWoodcutters() * economyConfig.getWoodPerWoodcutter();
        int stoneRate = pop.getStoneworkers() * economyConfig.getStonePerStoneworker();

        // 2. Add the earnings from the elapsed time into the 'Pending' pool
        res.setPendingFood(res.getPendingFood() + (int)(foodRate * hoursElapsed));
        res.setPendingWood(res.getPendingWood() + (int)(woodRate * hoursElapsed));
        res.setPendingStone(res.getPendingStone() + (int)(stoneRate * hoursElapsed));

        // 3. Update the timestamp so the next calculation starts from 'Now'
        res.setLastUpdate(now);

        profileRepository.save(profile);
    }

    /**
     * Moves all resources from the Pending pool into the Main Treasury (Official Balance).
     * This is the logic behind the 'Collect All' button in the UI.
     */
    @Transactional
    public void claimResources(PlayerProfile profile) {
        // First, ensure the pending pool is up-to-date with the current second
        updateProductionState(profile);

        PlayerResources res = profile.getResources();

        // Move Pending -> Treasury
        res.setFood(res.getFood() + res.getPendingFood());
        res.setWood(res.getWood() + res.getPendingWood());
        res.setStone(res.getStone() + res.getPendingStone());

        // Reset the pending pool back to zero
        res.setPendingFood(0);
        res.setPendingWood(0);
        res.setPendingStone(0);

        profileRepository.save(profile);
    }
}