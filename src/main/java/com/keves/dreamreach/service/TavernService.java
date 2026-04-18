package com.keves.dreamreach.service;

import com.keves.dreamreach.config.GameEconomyConfig;
import com.keves.dreamreach.entity.BuildingInstance;
import com.keves.dreamreach.entity.CharacterTemplate;
import com.keves.dreamreach.entity.PlayerProfile;
import com.keves.dreamreach.entity.RecruitmentPool;
import com.keves.dreamreach.entity.TavernListing;
import com.keves.dreamreach.repository.PlayerProfileRepository;
import com.keves.dreamreach.repository.RecruitmentPoolRepository;
import com.keves.dreamreach.repository.TavernListingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;

/**
 * Service handling the offline catch-up logic for Tavern hero arrivals.
 */
@Service
public class TavernService {

    private final GameEconomyConfig config;
    private final PlayerProfileRepository profileRepository;
    private final TavernListingRepository tavernListingRepository;
    private final RecruitmentPoolRepository recruitmentPoolRepository;
    private final Random random = new Random();

    public TavernService(GameEconomyConfig config,
                         PlayerProfileRepository profileRepository,
                         TavernListingRepository tavernListingRepository,
                         RecruitmentPoolRepository recruitmentPoolRepository) {
        this.config = config;
        this.profileRepository = profileRepository;
        this.tavernListingRepository = tavernListingRepository;
        this.recruitmentPoolRepository = recruitmentPoolRepository;
    }

    /**
     * Engine to process organic hero arrivals. Calculates missed intervals
     * since the player's last check to ensure fairness for offline time.
     */
    @Transactional
    public void processArrivals(PlayerProfile profile) {
        // Validation: Ensure player has reached the Keep level required for the Tavern
        int keepLevel = profile.getBuildings().stream()
                .filter(b -> b.getBuildingType().equalsIgnoreCase("keep"))
                .mapToInt(BuildingInstance::getLevel)
                .max().orElse(1);

        if (keepLevel < config.getTavernUnlockLevel()) {
            return;
        }

        Instant now = Instant.now();
        Instant lastCheck = profile.getLastTavernCheckTime();
        if (lastCheck == null) {
            lastCheck = now;
            profile.setLastTavernCheckTime(now);
        }

        long minutesPassed = Duration.between(lastCheck, now).toMinutes();
        long intervals = minutesPassed / config.getTavernCheckIntervalMinutes();

        if (intervals <= 0) {
            return; // Not enough time has passed for a new roll
        }

        TavernListing currentListing = tavernListingRepository.findByProfileId(profile.getId()).orElse(null);
        int intervalsProcessed = 0;

        for (int i = 0; i < intervals; i++) {
            intervalsProcessed++;

            // To simulate time accurately, we calculate the exact moment this roll occurred
            Instant simulatedTime = lastCheck.plus(Duration.ofMinutes((long) intervalsProcessed * config.getTavernCheckIntervalMinutes()));

            // Check if there is a hero and if they have expired during this simulated step
            if (currentListing != null) {
                if (simulatedTime.isAfter(currentListing.getExpiryTime())) {
                    tavernListingRepository.delete(currentListing);
                    currentListing = null;
                } else {
                    continue; // Slot is full and the hero is still waiting, skip this arrival roll
                }
            }

            // Slot is empty, roll for a new arrival
            if (random.nextDouble() <= config.getTavernArrivalChance()) {
                generateNewListing(profile, simulatedTime);
                break; // A hero arrived, stop evaluating further steps
            }
        }

        // Advance the check time by the intervals actually processed to save the remaining "unused" minutes
        profile.setLastTavernCheckTime(lastCheck.plus(Duration.ofMinutes((long) intervalsProcessed * config.getTavernCheckIntervalMinutes())));
        profileRepository.save(profile);
    }

    private void generateNewListing(PlayerProfile profile, Instant arrivalTime) {
        List<RecruitmentPool> pool = recruitmentPoolRepository.findAll();
        if (pool.isEmpty()) return;

        // Weighted random selection from the roster
        int totalWeight = pool.stream().mapToInt(RecruitmentPool::getWeight).sum();
        int roll = random.nextInt(totalWeight);

        RecruitmentPool selected = null;
        int currentWeight = 0;
        for (RecruitmentPool p : pool) {
            currentWeight += p.getWeight();
            if (roll < currentWeight) {
                selected = p;
                break;
            }
        }

        if (selected == null) {
            selected = pool.getLast();
        }

        CharacterTemplate template = selected.getCharacterTemplate();

        TavernListing listing = new TavernListing();
        listing.setProfile(profile);
        listing.setCharacterTemplate(template);
        listing.setArrivalTime(arrivalTime);
        listing.setExpiryTime(arrivalTime.plus(Duration.ofHours(config.getTavernStayDurationHours())));
        listing.setGoldCost(template.getBaseGoldCost());
        listing.setGemCost(template.getBaseGemCost());

        tavernListingRepository.save(listing);
    }
}