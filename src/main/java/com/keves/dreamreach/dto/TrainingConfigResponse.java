package com.keves.dreamreach.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TrainingConfigResponse {
    private String professionType;
    private int goldCost;
    private int foodCost;
    private int trainTimeSeconds;
}