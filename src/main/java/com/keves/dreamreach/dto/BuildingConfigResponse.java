package com.keves.dreamreach.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO to securely pass dynamic building costs and timers from the
 * GameEconomyConfig down to the React frontend.
 */
@Getter
@Setter
@Builder
public class BuildingConfigResponse {
    private String buildingType;
    private int woodCost;
    private int stoneCost;
    private int buildTimeSeconds;
    private int maxWorkers;
    private int productionRate;
}