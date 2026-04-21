package com.keves.dreamreach.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class PlayerProfileResponse {
    private String email;
    private String displayName;
    private boolean pvpEnabled;

    @JsonProperty("isAdmin")
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

    // Pending amounts for the Ledger. Now doubles to support fractional UI updates.
    private double pendingFood;
    private double pendingWood;
    private double pendingStone;
    private double pendingGold;

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

    private List<BuildingInstanceResponse> buildings;
    private List<ConstructionTaskResponse> activeConstructions;
    private List<TrainingTaskResponse> activeTrainingTasks;
    private List<TrainingConfigResponse> trainingConfigs;
    private List<BuildingConfigResponse> buildingConfigs;
    private List<LedgerEventResponse> ledgerEvents;

    // --- PROGRESSION GATING ---
    private List<UpgradeTaskResponse> activeUpgrades;
    private KeepUpgradeRequirementsResponse keepUpgradeRequirements;

    @Data
    @Builder
    public static class LedgerEventResponse {
        private String id;
        private long timestampEpoch;
        private String category;
        private String message;
    }
}