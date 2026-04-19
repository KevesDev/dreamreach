package com.keves.dreamreach.service;

import com.keves.dreamreach.config.GameEconomyConfig;
import com.keves.dreamreach.entity.PlayerPopulation;
import com.keves.dreamreach.entity.PlayerProfile;
import com.keves.dreamreach.entity.PlayerResources;
import com.keves.dreamreach.entity.TrainingTask;
import com.keves.dreamreach.repository.PlayerProfileRepository;
import com.keves.dreamreach.repository.TrainingTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class TrainingService {

    private final TrainingTaskRepository taskRepository;
    private final PlayerProfileRepository profileRepository;
    private final EconomyService economyService;
    private final GameEconomyConfig config;

    public TrainingService(TrainingTaskRepository taskRepository,
                           PlayerProfileRepository profileRepository,
                           EconomyService economyService,
                           GameEconomyConfig config) {
        this.taskRepository = taskRepository;
        this.profileRepository = profileRepository;
        this.economyService = economyService;
        this.config = config;
    }

    @Transactional
    public void queueTraining(PlayerProfile profile, String profession, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Must train at least 1 peasant.");
        economyService.updateProductionState(profile);

        PlayerPopulation pop = profile.getPopulation();
        if (pop.getIdlePeasants() < quantity) throw new IllegalStateException("Not enough Idle Peasants.");

        int unitGoldCost;
        int unitFoodCost;
        int unitTrainTimeSeconds;

        switch (profession.toLowerCase()) {
            case "woodcutter" -> {
                unitGoldCost = config.getCostTrainWoodcutterGold();
                unitFoodCost = config.getCostTrainWoodcutterFood();
                unitTrainTimeSeconds = config.getTrainTimeWoodcutterSeconds();
            }
            case "stoneworker" -> {
                unitGoldCost = config.getCostTrainStoneworkerGold();
                unitFoodCost = config.getCostTrainStoneworkerFood();
                unitTrainTimeSeconds = config.getTrainTimeStoneworkerSeconds();
            }
            case "hunter" -> {
                unitGoldCost = config.getCostTrainHunterGold();
                unitFoodCost = config.getCostTrainHunterFood();
                unitTrainTimeSeconds = config.getTrainTimeHunterSeconds();
            }
            case "baker" -> {
                unitGoldCost = config.getCostTrainBakerGold();
                unitFoodCost = config.getCostTrainBakerFood();
                unitTrainTimeSeconds = config.getTrainTimeBakerSeconds();
            }
            default -> throw new IllegalArgumentException("Unknown profession.");
        }

        PlayerResources res = profile.getResources();
        if (res.getGold() < (unitGoldCost * quantity) || res.getFood() < (unitFoodCost * quantity)) {
            throw new IllegalStateException("Not enough resources.");
        }

        res.setGold(res.getGold() - (unitGoldCost * quantity));
        res.setFood(res.getFood() - (unitFoodCost * quantity));
        pop.setIdlePeasants(pop.getIdlePeasants() - quantity);
        pop.setInTraining(pop.getInTraining() + quantity);

        List<TrainingTask> existingTasks = taskRepository.findByProfileIdOrderByStartTimeAsc(profile.getId());
        Instant lastCompletion = Instant.now();
        if (!existingTasks.isEmpty()) {
            Instant highestTime = existingTasks.getLast().getCompletionTime();
            if (highestTime.isAfter(lastCompletion)) {
                lastCompletion = highestTime;
            }
        }

        for (int i = 0; i < quantity; i++) {
            Instant end = lastCompletion.plusSeconds(unitTrainTimeSeconds);
            TrainingTask task = new TrainingTask();
            task.setProfile(profile);
            task.setProfessionType(profession.toLowerCase());
            task.setStartTime(lastCompletion);
            task.setCompletionTime(end);
            taskRepository.save(task);
            lastCompletion = end;
        }
        profileRepository.save(profile);
    }

    @Transactional
    public void processCompletedTraining(PlayerProfile profile) {
        List<TrainingTask> tasks = taskRepository.findByProfileIdOrderByStartTimeAsc(profile.getId());
        Instant now = Instant.now();
        PlayerPopulation pop = profile.getPopulation();
        boolean changed = false;

        int activeCount = 0;
        for (TrainingTask task : tasks) {
            if (now.isBefore(task.getCompletionTime())) {
                activeCount++;
                continue;
            }
            switch (task.getProfessionType().toLowerCase()) {
                case "woodcutter" -> pop.setWoodcutters(pop.getWoodcutters() + 1);
                case "stoneworker" -> pop.setStoneworkers(pop.getStoneworkers() + 1);
                case "hunter" -> pop.setHunters(pop.getHunters() + 1);
                case "baker" -> pop.setBakers(pop.getBakers() + 1);
            }
            taskRepository.delete(task);
            changed = true;
        }

        // Self-Healing: Refunds stranded peasants lost to concurrent UI race conditions
        if (pop.getInTraining() != activeCount) {
            int stranded = pop.getInTraining() - activeCount;
            pop.setInTraining(activeCount);
            pop.setIdlePeasants(Math.max(0, pop.getIdlePeasants() + stranded));
            changed = true;
        }

        if (changed) {
            profileRepository.save(profile);
        }
    }
}