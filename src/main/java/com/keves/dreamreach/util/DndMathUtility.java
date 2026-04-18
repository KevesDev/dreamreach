package com.keves.dreamreach.util;

public class DndMathUtility {

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
}