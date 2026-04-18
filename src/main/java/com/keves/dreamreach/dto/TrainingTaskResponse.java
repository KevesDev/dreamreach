package com.keves.dreamreach.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TrainingTaskResponse {
    private String id; // We need the ID so React knows exactly which task to "complete"
    private String professionType;
    private long startTimeEpoch;
    private long completionTimeEpoch;
}