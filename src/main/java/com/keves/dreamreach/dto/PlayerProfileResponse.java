package com.keves.dreamreach.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * DTO for sending the user's profile data to the React dashboard.
 * All dynamic economy configs are now passed here to prevent frontend hardcoding.
 */
@Getter
@Setter
@Builder
public class PlayerProfileResponse {
    private String email;
    private String displayName;
    private boolean pvpEnabled;
    private boolean isAdmin; // Tells the React UI to show the Admin tools

    // Resources
    private int food;
    private int wood;
    private int stone;
    private int gold;
    private int gems;

    // Rates for the HUD ticker
    private int foodRate;
    private int woodRate;
    private int stoneRate;

    // Pending amounts for the Ledger
    private int pendingFood;
    private int pendingWood;
    private int pendingStone;
    private int pendingGold;

    // Tax and Happiness Metrics
    private int happiness;
    private int maxHappiness;
    private String taxBracket;
    private long lastTaxCollectionTimeEpoch;

    // Population metrics needed for the HUD and Citizen Management
    private int totalPopulation;
    private int maxPopulation;

    // The specific breakdown of the population
    private int idlePeasants;
    private int woodcutters;
    private int stoneworkers;
    private int hunters;
    private int bakers;

    // Structure Data
    private int keepLevel;
    private int maxStorage;
    private int houses;
    private int towers;
    private int bakeries;
    private int huntingLodges;

    // List of real database building instances for the UI to map uniquely
    private List<BuildingInstanceResponse> buildings;

    private List<ConstructionTaskResponse> activeConstructions;
    private List<TrainingTaskResponse> activeTrainingTasks;
    private List<TrainingConfigResponse> trainingConfigs;

    // Added to pass dynamic building costs/timers to the UI
    private List<BuildingConfigResponse> buildingConfigs;
}