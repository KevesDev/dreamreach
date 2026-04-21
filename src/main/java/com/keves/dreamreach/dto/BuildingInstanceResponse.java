package com.keves.dreamreach.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class BuildingInstanceResponse {
    private UUID id;
    private String buildingType;
    private int level;
    private int assignedWorkers;

    // Server-Authoritative Math sent to the UI
    private int nextLevelWoodCost;
    private int nextLevelStoneCost;
    private int nextLevelTimeSeconds;
}