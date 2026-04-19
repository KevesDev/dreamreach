package com.keves.dreamreach.util;

import com.keves.dreamreach.enums.Rarity;
import java.util.*;

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
     * Generates a hero's stat block using Priority Mapping.
     * 1. Rolls 6 values using 3d6 (Standard) or 4d6 Drop Lowest (Heroic).
     * 2. Applies Rarity-based BST (Base Stat Total) floors.
     * 3. Clamps every stat to a minimum Heroic Floor of 8.
     * 4. Maps sorted values to the template priority list.
     */
    public static Map<String, Integer> generateHeroStats(List<String> priorityOrder, Rarity rarity) {
        List<Integer> pool = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            int roll;
            if (rarity == Rarity.COMMON || rarity == Rarity.UNCOMMON) {
                roll = rollNd6(3);
            } else {
                roll = roll4d6DropLowest();
            }
            pool.add(Math.max(8, roll)); // Heroic hard floor of 8
        }

        // Apply Rarity BST Floors to ensure quality scaling
        int bstFloor = switch (rarity) {
            case COMMON -> 60;
            case UNCOMMON -> 70;
            case RARE -> 78;
            case EPIC -> 84;
            case LEGENDARY -> 90;
        };

        int currentBst = pool.stream().mapToInt(Integer::intValue).sum();
        if (currentBst < bstFloor) {
            int deficit = bstFloor - currentBst;
            pool.sort(Collections.reverseOrder());
            // Buff the top stats to guarantee power where it matters
            pool.set(0, pool.get(0) + (deficit / 2));
            pool.set(1, pool.get(1) + (deficit / 2 + deficit % 2));
        }

        pool.sort(Collections.reverseOrder());
        Map<String, Integer> finalStats = new HashMap<>();

        // Map the sorted pool to the priority list defined in the Forge
        for (int i = 0; i < priorityOrder.size(); i++) {
            finalStats.put(priorityOrder.get(i).toUpperCase(), pool.get(i));
        }

        return finalStats;
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