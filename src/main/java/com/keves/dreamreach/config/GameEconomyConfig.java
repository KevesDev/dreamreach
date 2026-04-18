package com.keves.dreamreach.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "game.economy")
@Getter
@Setter
public class GameEconomyConfig {

    // --- STARTING CONDITIONS ---
    private int startingFood = 150;
    private int startingWood = 100;
    private int startingStone = 50;
    private int startingGold = 0;
    private int startingGems = 0;

    private int startingHappiness = 50;
    private int startingPeasants = 5;

    private int startingHouses = 1;
    private int startingTowers = 0;
    private int startingBakeries = 0;
    private int startingLodges = 0;

    // --- PASSIVE BASELINE ---
    private int basePassiveWood = 30;
    private int basePassiveStone = 30;

    // --- POPULATION RULES ---
    private int basePeasantCap = 10;
    private int capacityPerHouse = 5;
    private int maxHappiness = 100;

    // --- RNG POPULATION SIMULATION ---
    private int popTickIntervalMinutes = 15;
    private double baseMovementChance = 0.50;
    private double baseJoinWeight = 0.66;
    private double popChangePercentMin = 0.03;
    private double popChangePercentMax = 0.05;
    private double happinessImpactMultiplier = 0.01;

    // --- TAXES & HAPPINESS ---
    private double taxRateLowMultiplier = 0.5;
    private double taxRateNormalMultiplier = 1.0;
    private double taxRateHighMultiplier = 1.5;

    private int happinessModifierLowTax = 2;
    private int happinessModifierNormalTax = 0;
    private int happinessModifierHighTax = -5;

    private int taxGoldPerCitizenPerHour = 2;
    private int taxCooldownMinutes = 60;

    // --- CONSUMPTION RATES (Per Hour) ---
    private int foodConsumedPerPeasant = 2;

    // --- PRODUCTION RATES (Per Hour) ---
    private int foodPerHunter = 5;
    private int woodPerWoodcutter = 3;
    private int stonePerStoneworker = 2;
    private int foodPerBaker = 15;

    // --- BUILDING WORKER CAPACITIES ---
    private int maxWorkersBakery = 2;
    private int maxWorkersLodge = 2;

    // --- CONSTRUCTION BASE COSTS (Wood, Stone) ---
    // TODO: these might scale by level (e.g., cost * level^1.5)
    private int costHouseWood = 50;
    private int costHouseStone = 10;
    private int costBakeryWood = 100;
    private int costBakeryStone = 50;
    private int costLodgeWood = 120;
    private int costLodgeStone = 30;

    private int costTowerWood = 100;
    private int costTowerStone = 100;

    // The base construction cost for the Tavern
    private int costTavernWood = 200;
    private int costTavernStone = 150;

    // --- CONSTRUCTION BASE TIMERS (in seconds) ---
    private int buildTimeHouse = 60;     // 1 min
    private int buildTimeBakery = 300;   // 5 mins
    private int buildTimeTower = 300;
    private int buildTimeLodge = 300;    // 5 mins

    // The base construction time for the Tavern
    private int buildTimeTavern = 600;   // 10 mins

    // --- TAVERN RECRUITMENT LOGIC ---
    // The required Keep Level to construct or access the Tavern
    private int tavernUnlockLevel = 1;

    // How often (in minutes) the server calculates if a hero arrived
    private int tavernCheckIntervalMinutes = 1;

    // The percentage chance (0.0 to 1.0) per interval that an empty slot gets a hero
    private double tavernArrivalChance = 0.5;

    // How long (in hours) a hero will wait in the Tavern before leaving
    private int tavernStayDurationHours = 48;

    // Post-purchase Gacha Rarity Weights (e.g. 0.50 = 50% chance for Common)
    private double rarityWeightCommon = 0.50;
    private double rarityWeightUncommon = 0.30;
    private double rarityWeightRare = 0.14;
    private double rarityWeightEpic = 0.05;
    private double rarityWeightLegendary = 0.01;

    // --- TRAINING COSTS & TIMERS ---
    // Granular balancing for each profession type

    // Woodcutter
    private int costTrainWoodcutterGold = 10;
    private int costTrainWoodcutterFood = 25;
    private int trainTimeWoodcutterSeconds = 30;

    // Stoneworker
    private int costTrainStoneworkerGold = 15;
    private int costTrainStoneworkerFood = 30;
    private int trainTimeStoneworkerSeconds = 45;

    // Hunter
    private int costTrainHunterGold = 20;
    private int costTrainHunterFood = 0; // hunters cost 0 food in order to prevent a food outage soft-lock.
    private int trainTimeHunterSeconds = 60;

    // Baker
    private int costTrainBakerGold = 25;
    private int costTrainBakerFood = 10;
    private int trainTimeBakerSeconds = 90;

    // --- COMBAT / DEFENSE ---
    private int baseDefense = 10;
    private int defensePerTower = 25;

    // --- DAILY LOGIN REWARD CURVES ---
    private List<Integer> dailyResourceCurve = List.of(10, 20, 35, 55, 80, 110);
    private List<Integer> dailyGoldCurve = List.of(0, 0, 10, 25, 50, 100);
}