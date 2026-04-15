package com.keves.dreamreach.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.lang.reflect.Executable;
import java.util.UUID;

/**
 * An instance represents a specific character owned by the player, with persistent data.
 * It references the CharacterTemplate for base rules, but keeps its own progression state.
 */
@Entity
@Table(name = "player_character")
@Getter
@Setter
@NoArgsConstructor
public class PlayerCharacter {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // -----------------------------------------------------------------
    // RELATIONSHIPS
    // -----------------------------------------------------------------

    /**
     * Player that owns this character
     * Uses FetchType.LAZY, meaning Spring won't query the db for the PlayerProfile
     * until expressly calling getOwner(). Saves on memory.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private PlayerProfile owner;

    /**
     * Blueprint for this character
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private CharacterTemplate template;

    // -----------------------------------------------------------------
    // PROGRESSION
    // -----------------------------------------------------------------

    @Column(name = "current_level", nullable = false)
    private int currentLevel = 1;

    @Column(name = "current_xp", nullable = false)
    private int currentXp = 0;

    // -----------------------------------------------------------------
    // BONUS ATTRIBUTES (from leveling or RNG)
    // -----------------------------------------------------------------

    @Column(name = "bonus_str", nullable = false)
    private int bonusStr = 0;

    @Column(name = "bonus_dex", nullable = false)
    private int bonusDex = 0;

    @Column(name = "bonus_con", nullable = false)
    private int bonusCon = 0;

    @Column(name = "bonus_int", nullable = false)
    private int bonusInt = 0;

    @Column(name = "bonus_wis", nullable = false)
    private int bonusWis = 0;

    @Column(name = "bonus_cha", nullable = false)
    private int bonusCha = 0;

    // -----------------------------------------------------------------
    // BUSINESS LOGIC
    // -----------------------------------------------------------------

    /**
     * Calculates the total attributes by combining the Blueprint base and the instance bonus.
     * Uses standard DDD (Domain-Driven Design) by keeping logic inside the entity.
     */

    public int getTotalStrength() {
        return this.template.getBaseStr() + this.bonusStr;
    }
    public int getTotalDexterity() {
        return this.template.getBaseDex() + this.bonusDex;
    }
    public int getTotalConstitution() {
        return this.template.getBaseCon() + this.bonusCon;
    }
    public int getTotalIntelligence() {
        return this.template.getBaseInt() + this.bonusInt;
    }
    public int getTotalWisdom() {
        return this.template.getBaseWis() + this.bonusWis;
    }
    public int getTotalCharisma() {
        return this.template.getBaseCha() + this.bonusCha;
    }
}
