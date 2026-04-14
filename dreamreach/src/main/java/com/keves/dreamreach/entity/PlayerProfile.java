package com.keves.dreamreach.entity;

import jakarta.persistence.*;
import lombok.Generated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

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
}
