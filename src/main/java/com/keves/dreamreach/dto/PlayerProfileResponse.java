package com.keves.dreamreach.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for sending the user's profile data to the React dashboard.
 */
@Getter
@Setter
@Builder
public class PlayerProfileResponse {
    private String email;
    private String displayName;
    private boolean pvpEnabled;

    // Resources
    private int food;
    private int wood;
    private int stone;
    private int gold;
    private int gems;

    // Population metrics needed for the HUD
    private int totalPopulation;
    private int maxPopulation; // Derived from houses
}