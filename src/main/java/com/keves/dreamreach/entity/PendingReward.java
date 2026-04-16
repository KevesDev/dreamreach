package com.keves.dreamreach.entity;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * we use wrapper classes (Integer) instead of primitives (int).
 * This allows the entire object to safely be null in the
 * database when there are no pending rewards.
 */
@Embeddable
@Getter
@Setter
public class PendingReward {
    private Integer pendingFood;
    private Integer pendingWood;
    private Integer pendingStone;
    private Integer pendingGold;
    private Boolean pendingSummon;

    // The exact UTC date these numbers were rolled for.
    private LocalDate pendingDate;
}