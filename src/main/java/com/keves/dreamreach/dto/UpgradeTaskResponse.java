package com.keves.dreamreach.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UpgradeTaskResponse {
    private String buildingInstanceId;
    private String buildingType;
    private int targetLevel;
    private long startTimeEpoch;
    private long completionTimeEpoch;
}