package com.keves.dreamreach.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Returns the generated JWT back to the client upon successful authentication.
 * Includes daily streak data.
 */
@Getter
@Setter
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String type; // We will use this to specify "Bearer" token type

    @JsonProperty("isFirstLoginToday")
    private boolean isFirstLoginToday;

    private int consecutiveLogins;
    private List<DailyReward> rewardTrack;
}