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