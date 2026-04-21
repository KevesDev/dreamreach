package com.keves.dreamreach.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class KeepUpgradeRequirementsResponse {
    private int targetLevel;
    private int reqPopulation;
    private int currentPopulation;
    private int reqHeroCount;
    private int reqHeroLevel;
    private long currentValidHeroes;
    private int reqWood;
    private int reqStone;
    private int currentWood;
    private int currentStone;
    private int upgradeTimeSeconds;
}