package com.keves.dreamreach.service;

import com.keves.dreamreach.config.GameEconomyConfig;
import com.keves.dreamreach.dto.CharacterRosterResponse;
import com.keves.dreamreach.dto.TavernListingResponse;
import com.keves.dreamreach.entity.BuildingInstance;
import com.keves.dreamreach.entity.CharacterTemplate;
import com.keves.dreamreach.entity.PlayerProfile;
import com.keves.dreamreach.entity.PlayerResources;
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

@Service
public class TavernService {

    private final GameEconomyConfig config;
    private final PlayerProfileRepository profileRepository;
    private final TavernListingRepository tavernListingRepository;
    private final RecruitmentPoolRepository recruitmentPoolRepository;
    private final GachaService gachaService;
    private final Random random = new Random();

    public TavernService(GameEconomyConfig config,
                         PlayerProfileRepository profileRepository,
                         TavernListingRepository tavernListingRepository,
                         RecruitmentPoolRepository recruitmentPoolRepository,
                         GachaService gachaService) {
        this.config = config;
        this.profileRepository = profileRepository;
        this.tavernListingRepository = tavernListingRepository;
        this.recruitmentPoolRepository = recruitmentPoolRepository;
        this.gachaService = gachaService;
    }

    @Transactional(readOnly = true)
    public TavernListingResponse getActiveListing(PlayerProfile profile) {
        return tavernListingRepository.findByProfileId(profile.getId())
                .map(listing -> TavernListingResponse.builder()
                        .listingId(listing.getId())
                        .name(listing.getCharacterTemplate().getName())
                        .dndClass(listing.getCharacterTemplate().getDndClass().name())
                        .portraitUrl(listing.getCharacterTemplate().getPortraitUrl())
                        .goldCost(listing.getGoldCost())
                        .gemCost(listing.getGemCost())
                        .expiryTimeEpoch(listing.getExpiryTime().toEpochMilli())
                        .build())
                .orElse(null);
    }

    @Transactional
    public void processArrivals(PlayerProfile profile) {
        int keepLevel = profile.getBuildings().stream()
                .filter(b -> b.getBuildingType().equalsIgnoreCase("keep"))
                .mapToInt(BuildingInstance::getLevel)
                .max().orElse(1);

        if (keepLevel < config.getTavernUnlockLevel()) return;

        Instant now = Instant.now();
        Instant lastCheck = profile.getLastTavernCheckTime();
        if (lastCheck == null) {
            lastCheck = now;
            profile.setLastTavernCheckTime(now);
        }

        long minutesPassed = Duration.between(lastCheck, now).toMinutes();
        long intervals = minutesPassed / config.getTavernCheckIntervalMinutes();

        if (intervals <= 0) return;

        TavernListing currentListing = tavernListingRepository.findByProfileId(profile.getId()).orElse(null);
        int intervalsProcessed = 0;

        for (int i = 0; i < intervals; i++) {
            intervalsProcessed++;
            Instant simulatedTime = lastCheck.plus(Duration.ofMinutes((long) intervalsProcessed * config.getTavernCheckIntervalMinutes()));

            if (currentListing != null) {
                if (simulatedTime.isAfter(currentListing.getExpiryTime())) {
                    tavernListingRepository.delete(currentListing);
                    currentListing = null;
                } else {
                    continue;
                }
            }

            if (random.nextDouble() <= config.getTavernArrivalChance()) {
                generateNewListing(profile, simulatedTime);
                break;
            }
        }

        profile.setLastTavernCheckTime(lastCheck.plus(Duration.ofMinutes((long) intervalsProcessed * config.getTavernCheckIntervalMinutes())));
        profileRepository.save(profile);
    }

    private void generateNewListing(PlayerProfile profile, Instant arrivalTime) {
        List<RecruitmentPool> pool = recruitmentPoolRepository.findAll();
        if (pool.isEmpty()) return;

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

        if (selected == null) selected = pool.getLast();

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

    /**
     * Executes the Tavern transaction. Deducts currency, cleans up the listing,
     * and delegates the actual hero RNG generation to the universal GachaService.
     */
    @Transactional
    public CharacterRosterResponse recruitHero(PlayerProfile profile, String currencyType) {
        TavernListing listing = tavernListingRepository.findByProfileId(profile.getId())
                .orElseThrow(() -> new IllegalStateException("No hero is currently waiting in the Tavern."));

        if (listing.getExpiryTime().isBefore(Instant.now())) {
            tavernListingRepository.delete(listing);
            throw new IllegalStateException("The hero got tired of waiting and left the Tavern.");
        }

        PlayerResources resources = profile.getResources();
        if (currencyType.equalsIgnoreCase("gold")) {
            if (resources.getGold() < listing.getGoldCost()) throw new IllegalStateException("Not enough Gold.");
            resources.setGold(resources.getGold() - listing.getGoldCost());
        } else if (currencyType.equalsIgnoreCase("gems")) {
            if (resources.getGems() < listing.getGemCost()) throw new IllegalStateException("Not enough Gems.");
            resources.setGems(resources.getGems() - listing.getGemCost());
        } else {
            throw new IllegalArgumentException("Invalid currency type.");
        }

        // Save the deducted currency
        profileRepository.save(profile);

        // Remove the hero from the Tavern
        tavernListingRepository.delete(listing);

        // Delegate the core pulling logic to the Universal Engine
        return gachaService.pullCharacter(profile, listing.getCharacterTemplate());
    }
}