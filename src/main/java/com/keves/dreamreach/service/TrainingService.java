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
import java.util.UUID;

/**
 * Handles the logic and sequential queuing for Citizen Management.
 */
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
        if (quantity <= 0) {
            throw new IllegalArgumentException("Must train at least 1 peasant.");
        }

        // 1. Force a state flush so pending resources don't get lost
        economyService.updateProductionState(profile);

        PlayerPopulation pop = profile.getPopulation();
        if (pop.getIdlePeasants() < quantity) {
            throw new IllegalStateException("You do not have enough Idle Peasants to train.");
        }

        // 2. Dynamically determine the per-unit cost and time based on the profession
        int unitGoldCost = 0;
        int unitFoodCost = 0;
        int unitTrainTimeSeconds = 0;

        switch (profession.toLowerCase()) {
            case "woodcutter":
                unitGoldCost = config.getCostTrainWoodcutterGold();
                unitFoodCost = config.getCostTrainWoodcutterFood();
                unitTrainTimeSeconds = config.getTrainTimeWoodcutterSeconds();
                break;
            case "stoneworker":
                unitGoldCost = config.getCostTrainStoneworkerGold();
                unitFoodCost = config.getCostTrainStoneworkerFood();
                unitTrainTimeSeconds = config.getTrainTimeStoneworkerSeconds();
                break;
            case "hunter":
                unitGoldCost = config.getCostTrainHunterGold();
                unitFoodCost = config.getCostTrainHunterFood();
                unitTrainTimeSeconds = config.getTrainTimeHunterSeconds();
                break;
            case "baker":
                unitGoldCost = config.getCostTrainBakerGold();
                unitFoodCost = config.getCostTrainBakerFood();
                unitTrainTimeSeconds = config.getTrainTimeBakerSeconds();
                break;
            default:
                throw new IllegalArgumentException("Unknown profession type: " + profession);
        }

        int totalGoldCost = unitGoldCost * quantity;
        int totalFoodCost = unitFoodCost * quantity;

        PlayerResources res = profile.getResources();
        if (res.getGold() < totalGoldCost || res.getFood() < totalFoodCost) {
            throw new IllegalStateException("Not enough resources to train these peasants.");
        }

        // 3. Deduct resources and shift the peasants from 'Idle' to 'In Training'
        res.setGold(res.getGold() - totalGoldCost);
        res.setFood(res.getFood() - totalFoodCost);
        pop.setIdlePeasants(pop.getIdlePeasants() - quantity);
        pop.setInTraining(pop.getInTraining() + quantity);

        // 4. Sequential Queuing Logic: Find the latest completion time of existing tasks
        List<TrainingTask> existingTasks = taskRepository.findByProfileIdOrderByStartTimeAsc(profile.getId());
        Instant lastCompletion = Instant.now();

        if (!existingTasks.isEmpty()) {
            Instant highestExistingTime = existingTasks.get(existingTasks.size() - 1).getCompletionTime();
            if (highestExistingTime.isAfter(lastCompletion)) {
                lastCompletion = highestExistingTime;
            }
        }

        // 5. Generate individual tasks stacked back-to-back using the specific profession's timer
        for (int i = 0; i < quantity; i++) {
            Instant start = lastCompletion;
            Instant end = start.plusSeconds(unitTrainTimeSeconds);

            TrainingTask task = new TrainingTask();
            task.setProfile(profile);
            task.setProfessionType(profession.toLowerCase());
            task.setStartTime(start);
            task.setCompletionTime(end);
            taskRepository.save(task);

            // The next unit in the loop starts exactly when this one finishes
            lastCompletion = end;
        }

        profileRepository.save(profile);
    }

    @Transactional
    public void completeTraining(PlayerProfile profile, String taskId) {
        TrainingTask task = taskRepository.findById(UUID.fromString(taskId))
                .orElseThrow(() -> new IllegalStateException("Training task not found."));

        // Security check
        if (!task.getProfile().getId().equals(profile.getId())) {
            throw new IllegalStateException("You do not own this training task.");
        }

        if (Instant.now().isBefore(task.getCompletionTime())) {
            throw new IllegalStateException("This peasant has not finished training yet.");
        }

        PlayerPopulation pop = profile.getPopulation();

        // Safety check to ensure we don't drop below zero if something desynced
        if (pop.getInTraining() > 0) {
            pop.setInTraining(pop.getInTraining() - 1);
        }

        // Physically convert the unit into its new profession
        switch (task.getProfessionType().toLowerCase()) {
            case "woodcutter":
                pop.setWoodcutters(pop.getWoodcutters() + 1);
                break;
            case "stoneworker":
                pop.setStoneworkers(pop.getStoneworkers() + 1);
                break;
            case "hunter":
                pop.setHunters(pop.getHunters() + 1);
                break;
            case "baker":
                pop.setBakers(pop.getBakers() + 1);
                break;
            default:
                throw new IllegalStateException("Unknown profession type: " + task.getProfessionType());
        }

        // Remove the task from the queue and save
        taskRepository.delete(task);
        profileRepository.save(profile);
    }
}