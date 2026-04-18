package com.keves.dreamreach.entity;

import jakarta.persistence.*;
import lombok.Generated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;
import java.time.Instant;

/**
 * This represents the public-facing identity of a player within the game.
 * Seperated from authentication account to preserve data integrity.
 */

@Entity
@Table(name = "player_profile")
@Getter
@Setter
@NoArgsConstructor
public class PlayerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "display_name", unique = true, nullable = false, length = 50)
    private String displayName;

    @Column(name = "is_personal_pvp_enabled", nullable = false)
    private boolean isPersonalPvpEnabled = false;

    // --- NEW TAX & HAPPINESS FIELDS ---
    @Column(name = "happiness", nullable = false)
    private int happiness = 50;

    @Column(name = "tax_bracket", nullable = false, length = 20)
    private String taxBracket = "NORMAL"; // "LOW", "NORMAL", "HIGH"

    @Column(name = "last_tax_collection_time", nullable = false)
    private Instant lastTaxCollectionTime = Instant.now();

    // --- TAVERN CHECK TRACKING ---
    @Column(name = "last_tavern_check_time")
    private Instant lastTavernCheckTime = Instant.now();

    /**
     * The alliance this player is part of.
     * Using FetchType.LAZY makes it so we don't load the
     * whole alliance roster every time we load a single player.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alliance_id")
    private Alliance alliance;

    /**
     * Checks the pvp status of the player.
     * If they have an alliance, their alliance rules
     * supersede their personal flag.
     * @return true if vulnerable to pvp, false otherwise.
     */
    public boolean isEffectivelyPvpEnabled() {
        if (this.alliance != null) {
            return this.alliance.isAlliancePvpEnabled();
        }

        return this.isPersonalPvpEnabled;
    }

    /**
     * @JoinColumn: This physically creates the 'account_id' column
     * in the player_profile table. This is the "Bridge".
     */
    @OneToOne
    @JoinColumn(name = "account_id", nullable = false)
    private PlayerAccount account;

    @OneToOne(mappedBy = "profile", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private PlayerResources resources;

    @OneToOne(mappedBy = "profile", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private PlayerPopulation population;

    // Relational Architecture: One profile has many physical building instances
    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private java.util.List<BuildingInstance> buildings = new java.util.ArrayList<>();
}