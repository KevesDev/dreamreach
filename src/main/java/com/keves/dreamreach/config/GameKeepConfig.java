package com.keves.dreamreach.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "game.keep")
@Getter
@Setter
public class GameKeepConfig {

    // --- POPULATION SCALING ---
    // Formula: basePopulation * (targetLevel ^ populationExponent)
    private int basePopulation = 50;
    private double populationExponent = 1.5;

    // --- HERO ROSTER SCALING (Horizontal Progression) ---
    // Formula: baseHeroCount * (targetLevel ^ heroCountExponent)
    private int baseHeroCount = 1;
    private double heroCountExponent = 1.2;

    // --- HERO LEVEL SCALING (Vertical Progression) ---
    // Formula: baseHeroLevel * (targetLevel ^ heroLevelExponent)
    private int baseHeroLevel = 2;
    private double heroLevelExponent = 1.1;

    // --- RESOURCE COST SCALING ---
    // Formula: baseCost * (targetLevel ^ costExponent)
    private int baseWoodCost = 200;
    private int baseStoneCost = 200;
    private double costExponent = 1.5;

    // The Safety Valve: Mathematically guarantees that a Keep upgrade's cost
    // will never exceed the maximum storage cap of the player's CURRENT keep level.
    private double maxCostPercentageOfStorage = 0.90;

    // --- UPGRADE TIMER ---
    // Linear Scaling: baseKeepUpgradeTimeMinutes * targetLevel
    private int baseKeepUpgradeTimeMinutes = 60;
}