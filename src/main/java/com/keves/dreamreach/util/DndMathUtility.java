package com.keves.dreamreach.util;

import com.keves.dreamreach.enums.Rarity;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class DndMathUtility {

    private static final Random random = new Random();

    /**
     * Calculates the D&D 5e ability modifier from a raw score.
     */
    public static int calculateModifier(int stat) {
        return (int) Math.floor((stat - 10) / 2.0);
    }

    /**
     * Calculates the maximum HP according to 2024 D&D SRD rules using the average hit die.
     */
    public static int calculateMaxHp(int level, int hitDieType, int conModifier) {
        if (level < 1) return 0;
        int level1Hp = hitDieType + conModifier;
        int averageHitDie = (hitDieType / 2) + 1;
        int subsequentHp = (level - 1) * (averageHitDie + conModifier);
        return Math.max(1, level1Hp + subsequentHp);
    }

    /**
     * Maps an XP total to a D&D 5e Character Level.
     */
    public static int calculateLevelFromXp(int xp) {
        if (xp >= 355000) return 20;
        if (xp >= 305000) return 19;
        if (xp >= 265000) return 18;
        if (xp >= 225000) return 17;
        if (xp >= 195000) return 16;
        if (xp >= 165000) return 15;
        if (xp >= 140000) return 14;
        if (xp >= 120000) return 13;
        if (xp >= 100000) return 12;
        if (xp >= 85000) return 11;
        if (xp >= 64000) return 10;
        if (xp >= 48000) return 9;
        if (xp >= 34000) return 8;
        if (xp >= 23000) return 7;
        if (xp >= 14000) return 6;
        if (xp >= 6500) return 5;
        if (xp >= 2700) return 4;
        if (xp >= 900) return 3;
        if (xp >= 300) return 2;
        return 1;
    }

    /**
     * Generates a set of 6 D&D ability scores, scaled by the character's gacha rarity.
     * Higher rarities use generous rolling rules and guarantee a high primary stat.
     */
    public static Map<String, Integer> generateRolledStats(String primaryStat, Rarity rarity) {
        Map<String, Integer> stats = new HashMap<>();
        String[] attributes = {"STR", "DEX", "CON", "INT", "WIS", "CHA"};

        for (String attr : attributes) {
            int score;
            if (rarity == Rarity.COMMON || rarity == Rarity.UNCOMMON) {
                score = rollNd6(3); // Standard 3d6 (Mean ~10.5)
            } else {
                score = roll4d6DropLowest(); // Heroic 4d6 drop lowest (Mean ~12.2)
            }
            stats.put(attr, score);
        }

        // Enforce Rarity-based primary stat guarantees
        int currentPrimary = stats.getOrDefault(primaryStat, 10);
        switch (rarity) {
            case UNCOMMON:
                if (currentPrimary < 12) stats.put(primaryStat, 12);
                break;
            case RARE:
                if (currentPrimary < 14) stats.put(primaryStat, 14);
                break;
            case EPIC:
                if (currentPrimary < 16) stats.put(primaryStat, 16);
                break;
            case LEGENDARY:
                if (currentPrimary < 18) stats.put(primaryStat, 18);
                break;
            default:
                break;
        }

        return stats;
    }

    private static int rollNd6(int n) {
        int total = 0;
        for (int i = 0; i < n; i++) {
            total += random.nextInt(6) + 1;
        }
        return total;
    }

    private static int roll4d6DropLowest() {
        int min = 7;
        int total = 0;
        for (int i = 0; i < 4; i++) {
            int roll = random.nextInt(6) + 1;
            total += roll;
            if (roll < min) min = roll;
        }
        return total - min;
    }
}