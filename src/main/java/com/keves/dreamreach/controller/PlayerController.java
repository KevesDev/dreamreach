package com.keves.dreamreach.controller;

import com.keves.dreamreach.config.GameEconomyConfig;
import com.keves.dreamreach.dto.BuildingConfigResponse;
import com.keves.dreamreach.dto.ConstructionTaskResponse;
import com.keves.dreamreach.dto.PlayerProfileResponse;
import com.keves.dreamreach.dto.TrainingTaskResponse;
import com.keves.dreamreach.dto.TrainingConfigResponse;
import com.keves.dreamreach.entity.PlayerAccount;
import com.keves.dreamreach.entity.PlayerProfile;
import com.keves.dreamreach.entity.PlayerPopulation;
import com.keves.dreamreach.exception.ResourceNotFoundException;
import com.keves.dreamreach.repository.ConstructionTaskRepository;
import com.keves.dreamreach.repository.PlayerAccountRepository;
import com.keves.dreamreach.repository.TrainingTaskRepository;
import com.keves.dreamreach.service.ConstructionService;
import com.keves.dreamreach.service.EconomyService;
import com.keves.dreamreach.service.RewardService;
import com.keves.dreamreach.service.TrainingService;
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
    private final TrainingService trainingService;
    private final TrainingTaskRepository trainingTaskRepository;

    public PlayerController(PlayerAccountRepository accountRepository,
                            GameEconomyConfig economyConfig,
                            RewardService rewardService,
                            EconomyService economyService,
                            ConstructionService constructionService,
                            ConstructionTaskRepository constructionTaskRepository,
                            TrainingService trainingService,
                            TrainingTaskRepository trainingTaskRepository) {
        this.accountRepository = accountRepository;
        this.economyConfig = economyConfig;
        this.rewardService = rewardService;
        this.economyService = economyService;
        this.constructionService = constructionService;
        this.constructionTaskRepository = constructionTaskRepository;
        this.trainingService = trainingService;
        this.trainingTaskRepository = trainingTaskRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<PlayerProfileResponse> getMyProfile(Authentication authentication) {
        String email = authentication.getName();

        PlayerAccount account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        PlayerProfile profile = account.getProfile();

        economyService.updateProductionState(profile);

        PlayerPopulation pop = profile.getPopulation();

        int houseCount = (int) profile.getBuildings().stream()
                .filter(b -> b.getBuildingType().equalsIgnoreCase("house")).count();
        int calculatedMaxPop = houseCount * economyConfig.getCapacityPerHouse();

        int foodRate = economyService.calculateFoodRate(profile);
        int woodRate = (pop != null ? pop.getWoodcutters() * economyConfig.getWoodPerWoodcutter() : 0) + economyConfig.getBasePassiveWood();
        int stoneRate = (pop != null ? pop.getStoneworkers() * economyConfig.getStonePerStoneworker() : 0) + economyConfig.getBasePassiveStone();

        List<ConstructionTaskResponse> activeTasks = constructionTaskRepository.findByProfileId(profile.getId())
                .stream()
                .map(task -> ConstructionTaskResponse.builder()
                        .buildingType(task.getBuildingType())
                        .targetLevel(task.getTargetLevel())
                        .startTimeEpoch(task.getStartTime().toEpochMilli())
                        .completionTimeEpoch(task.getCompletionTime().toEpochMilli())
                        .build())
                .collect(Collectors.toList());

        List<TrainingTaskResponse> activeTrainingTasks = trainingTaskRepository.findByProfileIdOrderByStartTimeAsc(profile.getId())
                .stream()
                .map(task -> TrainingTaskResponse.builder()
                        .id(task.getId().toString())
                        .professionType(task.getProfessionType())
                        .startTimeEpoch(task.getStartTime().toEpochMilli())
                        .completionTimeEpoch(task.getCompletionTime().toEpochMilli())
                        .build())
                .collect(Collectors.toList());

        // Extract backend config dynamically for the frontend dashboard
        List<TrainingConfigResponse> trainingConfigs = List.of(
                TrainingConfigResponse.builder().professionType("woodcutter")
                        .goldCost(economyConfig.getCostTrainWoodcutterGold())
                        .foodCost(economyConfig.getCostTrainWoodcutterFood())
                        .trainTimeSeconds(economyConfig.getTrainTimeWoodcutterSeconds()).build(),
                TrainingConfigResponse.builder().professionType("stoneworker")
                        .goldCost(economyConfig.getCostTrainStoneworkerGold())
                        .foodCost(economyConfig.getCostTrainStoneworkerFood())
                        .trainTimeSeconds(economyConfig.getTrainTimeStoneworkerSeconds()).build(),
                TrainingConfigResponse.builder().professionType("hunter")
                        .goldCost(economyConfig.getCostTrainHunterGold())
                        .foodCost(economyConfig.getCostTrainHunterFood())
                        .trainTimeSeconds(economyConfig.getTrainTimeHunterSeconds()).build(),
                TrainingConfigResponse.builder().professionType("baker")
                        .goldCost(economyConfig.getCostTrainBakerGold())
                        .foodCost(economyConfig.getCostTrainBakerFood())
                        .trainTimeSeconds(economyConfig.getTrainTimeBakerSeconds()).build()
        );

        List<BuildingConfigResponse> buildingConfigs = List.of(
                BuildingConfigResponse.builder().buildingType("house")
                        .woodCost(economyConfig.getCostHouseWood())
                        .stoneCost(economyConfig.getCostHouseStone())
                        .buildTimeSeconds(economyConfig.getBuildTimeHouse())
                        .maxWorkers(0)
                        .productionRate(0).build(),
                BuildingConfigResponse.builder().buildingType("bakery")
                        .woodCost(economyConfig.getCostBakeryWood())
                        .stoneCost(economyConfig.getCostBakeryStone())
                        .buildTimeSeconds(economyConfig.getBuildTimeBakery())
                        .maxWorkers(economyConfig.getMaxWorkersBakery())
                        .productionRate(economyConfig.getFoodPerBaker()).build(),
                BuildingConfigResponse.builder().buildingType("lodge")
                        .woodCost(economyConfig.getCostLodgeWood())
                        .stoneCost(economyConfig.getCostLodgeStone())
                        .buildTimeSeconds(economyConfig.getBuildTimeLodge())
                        .maxWorkers(economyConfig.getMaxWorkersLodge())
                        .productionRate(economyConfig.getFoodPerHunter()).build(),
                BuildingConfigResponse.builder().buildingType("tower")
                        .woodCost(economyConfig.getCostTowerWood())
                        .stoneCost(economyConfig.getCostTowerStone())
                        .buildTimeSeconds(economyConfig.getBuildTimeTower())
                        .maxWorkers(0)
                        .productionRate(0).build()
        );

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
                .pendingGold(profile.getResources() != null ? profile.getResources().getPendingGold() : 0)

                .happiness(profile.getHappiness())
                .maxHappiness(economyConfig.getMaxHappiness())
                .taxBracket(profile.getTaxBracket())
                .lastTaxCollectionTimeEpoch(profile.getLastTaxCollectionTime().toEpochMilli())

                .totalPopulation(profile.getPopulation() != null ? profile.getPopulation().getTotalPopulation() : 0)
                .maxPopulation(calculatedMaxPop)

                .idlePeasants(pop != null ? pop.getIdlePeasants() : 0)
                .woodcutters(pop != null ? pop.getWoodcutters() : 0)
                .stoneworkers(pop != null ? pop.getStoneworkers() : 0)
                .hunters(pop != null ? pop.getHunters() : 0)
                .bakers(pop != null ? pop.getBakers() : 0)

                .keepLevel(1)
                .houses(houseCount)
                .towers((int) profile.getBuildings().stream().filter(b -> b.getBuildingType().equalsIgnoreCase("tower")).count())
                .bakeries((int) profile.getBuildings().stream().filter(b -> b.getBuildingType().equalsIgnoreCase("bakery")).count())
                .huntingLodges((int) profile.getBuildings().stream().filter(b -> b.getBuildingType().equalsIgnoreCase("lodge")).count())

                .activeConstructions(activeTasks)
                .activeTrainingTasks(activeTrainingTasks)
                .trainingConfigs(trainingConfigs)
                .buildingConfigs(buildingConfigs)
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

    @PostMapping("/train")
    public ResponseEntity<?> queueTraining(Authentication authentication,
                                           @RequestParam String profession,
                                           @RequestParam int quantity) {
        PlayerAccount account = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        try {
            trainingService.queueTraining(account.getProfile(), profession, quantity);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/train/complete")
    public ResponseEntity<?> completeTraining(Authentication authentication, @RequestParam String taskId) {
        PlayerAccount account = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        try {
            trainingService.completeTraining(account.getProfile(), taskId);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- TAX ENDPOINTS ---

    @PostMapping("/taxes/bracket")
    public ResponseEntity<?> setTaxBracket(Authentication authentication, @RequestParam String bracket) {
        PlayerAccount account = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        try {
            economyService.setTaxBracket(account.getProfile(), bracket);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/taxes/collect")
    public ResponseEntity<?> collectTaxes(Authentication authentication) {
        PlayerAccount account = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        try {
            economyService.collectTaxes(account.getProfile());
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}