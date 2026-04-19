package com.keves.dreamreach.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ActiveMissionResponse {
    private UUID missionId;
    private String questTitle;
    private String questType;
    private int successChance;
    private long dispatchTimeEpoch;
    private long endTimeEpoch;

    @JsonProperty("isResolved")
    private boolean isResolved;

    @JsonProperty("wasSuccessful")
    private boolean wasSuccessful;

    // Added reward fields to ensure the UI can display results even after mission is removed from Journal
    private int rewardGold;
    private int rewardGems;
    private int rewardFood;
    private int rewardWood;
    private int rewardStone;
    private int rewardExp;

    private List<CharacterSnippet> partyMembers;

    @Data
    @Builder
    public static class CharacterSnippet {
        private UUID characterId;
        private String name;
        private String portraitUrl;
        private String flavorQuipsJson;
    }
}