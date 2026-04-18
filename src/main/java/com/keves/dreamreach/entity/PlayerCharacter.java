package com.keves.dreamreach.entity;

import com.keves.dreamreach.enums.Rarity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private PlayerProfile owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private CharacterTemplate template;

    // -----------------------------------------------------------------
    // PROGRESSION & VITALS (The Digital Character Sheet)
    // -----------------------------------------------------------------

    @Column(name = "current_level", nullable = false)
    private int currentLevel = 1;

    @Column(name = "current_xp", nullable = false)
    private int currentXp = 0;

    @Column(name = "current_hp", nullable = false)
    private int currentHp = 10;

    @Column(name = "max_hp", nullable = false)
    private int maxHp = 10;

    @Column(name = "current_mana", nullable = false)
    private int currentMana = 0;

    @Column(name = "max_mana", nullable = false)
    private int maxMana = 0;

    @Column(name = "spent_hit_dice", nullable = false)
    private int spentHitDice = 0;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "IDLE";

    @Column(name = "long_rest_end_time")
    private Instant longRestEndTime;

    @Column(name = "weapon_tier", nullable = false, length = 20)
    private String weaponTier = "BASIC";

    @Column(name = "armor_tier", nullable = false, length = 20)
    private String armorTier = "BASIC";

    // -----------------------------------------------------------------
    // ROLLED PROPERTIES (Overriding Template)
    // -----------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "rolled_rarity")
    private Rarity rolledRarity;

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