package com.keves.dreamreach.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

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

    // Base Stats
    private int totalStrength;
    private int totalDexterity;
    private int totalConstitution;
    private int totalIntelligence;
    private int totalWisdom;
    private int totalCharisma;

    // Computed Modifiers
    private int strMod;
    private int dexMod;
    private int conMod;
    private int intMod;
    private int wisMod;
    private int chaMod;

    // Vitals and Status
    private int currentHp;
    private int maxHp;
    private int spentHitDice;
    private String status;
    private String weaponTier;
    private String armorTier;
    private String portraitUrl;
}