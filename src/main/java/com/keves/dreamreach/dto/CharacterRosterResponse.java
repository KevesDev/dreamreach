package com.keves.dreamreach.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Data Transfer Object.
 * This is a flat representation of a character formatted for frontend display.
 * Prevents infinite recursion from JPA bidirectional relationships -
 * ie PlayerProfile.alliance -> Alliance.members -> PlayerProfile
 */
@Getter
@Setter
@Builder
public class CharacterRosterResponse {
    private UUID characterId;
    private String name;
    private String rarity;
    private String dndClass;
    private int level;
    private int currentXp;
    private int totalStrength;
    private int totalDexterity;
    private int totalConstitution;
    private int totalIntelligence;
    private int totalWisdom;
    private int totalCharisma;
}
