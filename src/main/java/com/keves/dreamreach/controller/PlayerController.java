package com.keves.dreamreach.controller;

import com.keves.dreamreach.config.GameEconomyConfig;
import com.keves.dreamreach.dto.ConstructionTaskResponse;
import com.keves.dreamreach.dto.PlayerProfileResponse;
import com.keves.dreamreach.entity.PlayerAccount;
import com.keves.dreamreach.entity.PlayerProfile;
import com.keves.dreamreach.entity.PlayerPopulation;
import com.keves.dreamreach.exception.ResourceNotFoundException;
import com.keves.dreamreach.repository.ConstructionTaskRepository;
import com.keves.dreamreach.repository.PlayerAccountRepository;
import com.keves.dreamreach.service.ConstructionService;
import com.keves.dreamreach.service.EconomyService;
import com.keves.dreamreach.service.RewardService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/player")
public class PlayerController {

    private final PlayerAccountRepository accountRepository;
    private final GameEconomyConfig economyConfig;
    private final RewardService rewardService;
    private final EconomyService economyService;
    private final ConstructionService constructionService;
    private final ConstructionTaskRepository constructionTaskRepository;

    private static final int BASE_WOOD_RATE = 12;
    private static final int BASE_STONE_RATE = 12;

    public PlayerController(PlayerAccountRepository accountRepository,
                            GameEconomyConfig economyConfig,
                            RewardService rewardService,
                            EconomyService economyService,
                            ConstructionService constructionService,
                            ConstructionTaskRepository constructionTaskRepository) {
        this.accountRepository = accountRepository;
        this.economyConfig = economyConfig;
        this.rewardService = rewardService;
        this.economyService = economyService;
        this.constructionService = constructionService;
        this.constructionTaskRepository = constructionTaskRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<PlayerProfileResponse> getMyProfile(Authentication authentication) {
        String email = authentication.getName();

        PlayerAccount account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        PlayerProfile profile = account.getProfile();

        economyService.updateProductionState(profile);

        PlayerPopulation pop = profile.getPopulation();

        int calculatedMaxPop = (profile.getStructures() != null)
                ? (profile.getStructures().getHouses() * economyConfig.getCapacityPerHouse())
                : 0;

        int foodRate = economyService.calculateFoodRate(profile);
        int woodRate = (pop != null ? pop.getWoodcutters() * economyConfig.getWoodPerWoodcutter() : 0) + BASE_WOOD_RATE;
        int stoneRate = (pop != null ? pop.getStoneworkers() * economyConfig.getStonePerStoneworker() : 0) + BASE_STONE_RATE;

        // SAFELY MAP TASKS: Uses the new findByProfileId repository method
        List<ConstructionTaskResponse> activeTasks = constructionTaskRepository.findByProfileId(profile.getId())
                .stream()
                .map(task -> ConstructionTaskResponse.builder()
                        .buildingType(task.getBuildingType()) // Requires @Getter or @Data in ConstructionTask.java
                        .targetLevel(task.getTargetLevel())
                        .startTimeEpoch(task.getStartTime().toEpochMilli())
                        .completionTimeEpoch(task.getCompletionTime().toEpochMilli())
                        .build())
                .collect(Collectors.toList());

        PlayerProfileResponse response = PlayerProfileResponse.builder()
                .email(account.getEmail())
                .displayName(account.getProfile().getDisplayName())
                .pvpEnabled(account.getProfile().isEffectivelyPvpEnabled())

                .food(profile.getResources() != null ? profile.getResources().getFood() : 0)
                .wood(profile.getResources() != null ? profile.getResources().getWood() : 0)
                .stone(profile.getResources() != null ? profile.getResources().getStone() : 0)
                .gold(profile.getResources() != null ? profile.getResources().getGold() : 0)
                .gems(profile.getResources() != null ? profile.getResources().getGems() : 0)

                .foodRate(foodRate)
                .woodRate(woodRate)
                .stoneRate(stoneRate)

                .pendingFood(profile.getResources() != null ? profile.getResources().getPendingFood() : 0)
                .pendingWood(profile.getResources() != null ? profile.getResources().getPendingWood() : 0)
                .pendingStone(profile.getResources() != null ? profile.getResources().getPendingStone() : 0)

                .totalPopulation(profile.getPopulation() != null ? profile.getPopulation().getTotalPopulation() : 0)
                .maxPopulation(calculatedMaxPop)

                // Ensure PlayerProfileResponse.java has: private List<ConstructionTaskResponse> activeConstructions;
                .activeConstructions(activeTasks)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/claim")
    public ResponseEntity<?> claimResources(Authentication authentication) {
        PlayerAccount account = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        economyService.claimResources(account.getProfile());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reward/claim")
    public ResponseEntity<?> claimDailyReward(Authentication authentication) {
        PlayerAccount account = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        try {
            rewardService.claimReward(account);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/construct")
    public ResponseEntity<?> startConstruction(Authentication authentication, @RequestParam String buildingType) {
        PlayerAccount account = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        try {
            constructionService.startConstruction(account.getProfile(), buildingType);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/construct/complete")
    public ResponseEntity<?> completeConstruction(Authentication authentication, @RequestParam String buildingType) {
        PlayerAccount account = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        try {
            constructionService.completeConstruction(account.getProfile(), buildingType);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}