package com.keves.dreamreach.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConstructionTaskResponse {
    private String buildingType;
    private int targetLevel;
    private long startTimeEpoch;
    private long completionTimeEpoch;
}