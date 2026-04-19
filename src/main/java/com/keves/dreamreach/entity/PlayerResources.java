package com.keves.dreamreach.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
public class PlayerResources {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false) private int food = 0;
    @Column(nullable = false) private int wood = 0;
    @Column(nullable = false) private int stone = 0;
    @Column(nullable = false) private int gold = 0;
    @Column(nullable = false) private int gems = 0;

    // pending resource pool (accrued but not claimed yet)
    @Column(nullable = false) private double pendingFood = 0.0;
    @Column(nullable = false) private double pendingWood = 0.0;
    @Column(nullable = false) private double pendingStone = 0.0;
    @Column(nullable = false) private double pendingGold = 0.0;

    // The last time the pending pool was calculated
    @Column(nullable = false)
    private Instant lastUpdate = Instant.now();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false, unique = true)
    private PlayerProfile profile;
}