package com.keves.dreamreach.dto;

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