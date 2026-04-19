package com.keves.dreamreach.controller;

import com.keves.dreamreach.config.GameEconomyConfig;
import com.keves.dreamreach.dto.*;
import com.keves.dreamreach.entity.BuildingInstance;
import com.keves.dreamreach.entity.PlayerAccount;
import com.keves.dreamreach.entity.PlayerProfile;
import com.keves.dreamreach.entity.PlayerPopulation;
import com.keves.dreamreach.exception.ResourceNotFoundException;
import com.keves.dreamreach.repository.ConstructionTaskRepository;
import com.keves.dreamreach.repository.PlayerAccountRepository;
import com.keves.dreamreach.repository.TrainingTaskRepository;
import com.keves.dreamreach.service.AccountCleanupService;
import com.keves.dreamreach.service.EconomyService;
import com.keves.dreamreach.service.TavernService;
import com.keves.dreamreach.service.TrainingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    private final EconomyService economyService;
    private final ConstructionTaskRepository constructionTaskRepository;
    private final TrainingTaskRepository trainingTaskRepository;
    private final TavernService tavernService;
    private final AccountCleanupService cleanupService;
    private final TrainingService trainingService;

    public PlayerController(PlayerAccountRepository accountRepository, GameEconomyConfig economyConfig,
                            EconomyService economyService, ConstructionTaskRepository constructionTaskRepository,
                            TrainingTaskRepository trainingTaskRepository, TavernService tavernService,
                            AccountCleanupService cleanupService, TrainingService trainingService) {
        this.accountRepository = accountRepository; this.economyConfig = economyConfig;
        this.economyService = economyService; this.constructionTaskRepository = constructionTaskRepository;
        this.trainingTaskRepository = trainingTaskRepository; this.tavernService = tavernService;
        this.cleanupService = cleanupService; this.trainingService = trainingService;
    }

    @GetMapping("/me")
    public ResponseEntity<PlayerProfileResponse> getMyProfile(Authentication authentication) {
        PlayerAccount account = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));
        PlayerProfile profile = account.getProfile();

        economyService.updateProductionState(profile);
        tavernService.processArrivals(profile);
        trainingService.processCompletedTraining(profile);

        PlayerPopulation pop = profile.getPopulation();
        int keepLevel = profile.getBuildings().stream().filter(b -> b.getBuildingType().equalsIgnoreCase("keep")).mapToInt(BuildingInstance::getLevel).max().orElse(1);
        int maxStorage = keepLevel * economyConfig.getBaseStoragePerKeepLevel();
        int houseCount = (int) profile.getBuildings().stream().filter(b -> b.getBuildingType().equalsIgnoreCase("house")).count();
        int calculatedMaxPop = houseCount * economyConfig.getCapacityPerHouse();
        int foodRate = economyService.calculateFoodRate(profile);
        int woodRate = (pop != null ? pop.getWoodcutters() * economyConfig.getWoodPerWoodcutter() : 0) + economyConfig.getBasePassiveWood();
        int stoneRate = (pop != null ? pop.getStoneworkers() * economyConfig.getStonePerStoneworker() : 0) + economyConfig.getBasePassiveStone();

        List<BuildingInstanceResponse> buildingResponses = profile.getBuildings().stream().map(b -> BuildingInstanceResponse.builder().id(b.getId()).buildingType(b.getBuildingType()).level(b.getLevel()).assignedWorkers(b.getAssignedWorkers()).build()).collect(Collectors.toList());
        List<ConstructionTaskResponse> activeTasks = constructionTaskRepository.findByProfileId(profile.getId()).stream().map(task -> ConstructionTaskResponse.builder().buildingType(task.getBuildingType()).targetLevel(task.getTargetLevel()).startTimeEpoch(task.getStartTime().toEpochMilli()).completionTimeEpoch(task.getCompletionTime().toEpochMilli()).build()).collect(Collectors.toList());
        List<TrainingTaskResponse> activeTrainingTasks = trainingTaskRepository.findByProfileIdOrderByStartTimeAsc(profile.getId()).stream().map(task -> TrainingTaskResponse.builder().id(task.getId().toString()).professionType(task.getProfessionType()).startTimeEpoch(task.getStartTime().toEpochMilli()).completionTimeEpoch(task.getCompletionTime().toEpochMilli()).build()).collect(Collectors.toList());

        List<TrainingConfigResponse> trainingConfigs = List.of(
                TrainingConfigResponse.builder().professionType("woodcutter").goldCost(economyConfig.getCostTrainWoodcutterGold()).foodCost(economyConfig.getCostTrainWoodcutterFood()).trainTimeSeconds(economyConfig.getTrainTimeWoodcutterSeconds()).build(),
                TrainingConfigResponse.builder().professionType("stoneworker").goldCost(economyConfig.getCostTrainStoneworkerGold()).foodCost(economyConfig.getCostTrainStoneworkerFood()).trainTimeSeconds(economyConfig.getTrainTimeStoneworkerSeconds()).build(),
                TrainingConfigResponse.builder().professionType("hunter").goldCost(economyConfig.getCostTrainHunterGold()).foodCost(economyConfig.getCostTrainHunterFood()).trainTimeSeconds(economyConfig.getTrainTimeHunterSeconds()).build(),
                TrainingConfigResponse.builder().professionType("baker").goldCost(economyConfig.getCostTrainBakerGold()).foodCost(economyConfig.getCostTrainBakerFood()).trainTimeSeconds(economyConfig.getTrainTimeBakerSeconds()).build()
        );

        List<BuildingConfigResponse> buildingConfigs = List.of(
                BuildingConfigResponse.builder().buildingType("house").woodCost(economyConfig.getCostHouseWood()).stoneCost(economyConfig.getCostHouseStone()).buildTimeSeconds(economyConfig.getBuildTimeHouse()).maxWorkers(0).productionRate(0).unlockKeepLevel(1).build(),
                BuildingConfigResponse.builder().buildingType("bakery").woodCost(economyConfig.getCostBakeryWood()).stoneCost(economyConfig.getCostBakeryStone()).buildTimeSeconds(economyConfig.getBuildTimeBakery()).maxWorkers(economyConfig.getMaxWorkersBakery()).productionRate(economyConfig.getFoodPerBaker()).unlockKeepLevel(1).build(),
                BuildingConfigResponse.builder().buildingType("lodge").woodCost(economyConfig.getCostLodgeWood()).stoneCost(economyConfig.getCostLodgeStone()).buildTimeSeconds(economyConfig.getBuildTimeLodge()).maxWorkers(economyConfig.getMaxWorkersLodge()).productionRate(economyConfig.getFoodPerHunter()).unlockKeepLevel(1).build(),
                BuildingConfigResponse.builder().buildingType("tavern").woodCost(economyConfig.getCostTavernWood()).stoneCost(economyConfig.getCostTavernStone()).buildTimeSeconds(economyConfig.getBuildTimeTavern()).maxWorkers(0).productionRate(0).unlockKeepLevel(economyConfig.getTavernUnlockLevel()).build()
        );

        PlayerProfileResponse response = PlayerProfileResponse.builder()
                .email(account.getEmail()).displayName(account.getProfile().getDisplayName()).pvpEnabled(account.getProfile().isEffectivelyPvpEnabled()).isAdmin(account.isAdmin())
                .food(profile.getResources() != null ? profile.getResources().getFood() : 0).wood(profile.getResources() != null ? profile.getResources().getWood() : 0).stone(profile.getResources() != null ? profile.getResources().getStone() : 0).gold(profile.getResources() != null ? profile.getResources().getGold() : 0).gems(profile.getResources() != null ? profile.getResources().getGems() : 0)
                .foodRate(foodRate).woodRate(woodRate).stoneRate(stoneRate)
                .pendingFood(profile.getResources() != null ? profile.getResources().getPendingFood() : 0).pendingWood(profile.getResources() != null ? profile.getResources().getPendingWood() : 0).pendingStone(profile.getResources() != null ? profile.getResources().getPendingStone() : 0).pendingGold(profile.getResources() != null ? profile.getResources().getPendingGold() : 0)
                .happiness(profile.getHappiness()).maxHappiness(economyConfig.getMaxHappiness())
                .taxBracket(profile.getTaxBracket()).lastTaxCollectionTimeEpoch(profile.getLastTaxCollectionTime() != null ? profile.getLastTaxCollectionTime().toEpochMilli() : 0)
                .totalPopulation(profile.getPopulation() != null ? profile.getPopulation().getTotalPopulation() : 0).maxPopulation(calculatedMaxPop)
                .idlePeasants(pop != null ? pop.getIdlePeasants() : 0).woodcutters(pop != null ? pop.getWoodcutters() : 0).stoneworkers(pop != null ? pop.getStoneworkers() : 0).hunters(pop != null ? pop.getHunters() : 0).bakers(pop != null ? pop.getBakers() : 0)
                .keepLevel(keepLevel).maxStorage(maxStorage).houses(houseCount)
                .buildings(buildingResponses).activeConstructions(activeTasks).activeTrainingTasks(activeTrainingTasks)
                .trainingConfigs(trainingConfigs).buildingConfigs(buildingConfigs)
                .build();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/me")
    public ResponseEntity<?> deleteMyAccount(Authentication authentication) {
        PlayerAccount account = accountRepository.findByEmail(authentication.getName()).orElseThrow(() -> new ResourceNotFoundException("Account not found."));
        cleanupService.deleteAccountAndAllRelationalData(account);
        return ResponseEntity.ok().build();
    }
}