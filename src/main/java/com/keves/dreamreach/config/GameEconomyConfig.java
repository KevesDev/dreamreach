package com.keves.dreamreach.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The Centralized Economy Engine Ruleset.
 * These defaults can be safely overwritten in application.properties
 * without requiring code recompilation.
 */
@Component
@ConfigurationProperties(prefix = "game.economy")
@Getter
@Setter
public class GameEconomyConfig {

    // --- POPULATION RULES ---
    private int basePeasantCap = 10;
    private int capacityPerHouse = 5;
    private int maxHappiness = 100;
    private int peasantJoinThreshold = 75; // Happiness needed for peasants to naturally join
    private int peasantLeaveThreshold = 25; // Happiness level where peasants abandon the settlement

    // --- CONSUMPTION RATES (Per Hour) ---
    private int foodConsumedPerPeasant = 2;

    // --- PRODUCTION RATES (Per Hour) ---
    private int foodPerHunter = 5;
    private int woodPerWoodcutter = 3;
    private int stonePerStoneworker = 2;
    private int foodPerBakery = 15; // Advanced structure production

    // --- COMBAT / DEFENSE ---
    private int baseDefense = 10;
    private int defensePerTower = 25;

    // --- DAILY LOGIN REWARD CURVES ---
    // Maps to Days 1 through 6. Day 7 is handled independently as the Summoning drop.
    private List<Integer> dailyResourceCurve = List.of(10, 20, 35, 55, 80, 110);
    private List<Integer> dailyGoldCurve = List.of(0, 0, 10, 25, 50, 100);
}