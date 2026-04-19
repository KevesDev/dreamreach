package com.keves.dreamreach.dto;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class PartySaveRequest {
    private List<UUID> characterIds;
}