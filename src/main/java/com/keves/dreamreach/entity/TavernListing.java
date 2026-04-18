package com.keves.dreamreach.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tavern_listing")
@Getter
@Setter
@NoArgsConstructor
public class TavernListing {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id = UUID.randomUUID();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false, unique = true)
    private PlayerProfile profile;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "character_template_id", nullable = false)
    private CharacterTemplate characterTemplate;

    @Column(name = "arrival_time", nullable = false)
    private Instant arrivalTime;

    @Column(name = "expiry_time", nullable = false)
    private Instant expiryTime;

    @Column(name = "gold_cost", nullable = false)
    private int goldCost;

    @Column(name = "gem_cost", nullable = false)
    private int gemCost;
}