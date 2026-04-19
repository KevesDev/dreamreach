package com.keves.dreamreach.dto;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class PartyCalculateRequest {
    private List<UUID> characterIds;
    private UUID questId;
}