package com.keves.dreamreach.entity;

import com.keves.dreamreach.enums.DndClass;
import com.keves.dreamreach.enums.Rarity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * The read-only blueprint for a character.
 * This defines the base rules and stats before any player rng or leveling is applied.
 */
@Entity
@Table(name = "character_template")
@Getter
@Setter
@NoArgsConstructor
public class CharacterTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", unique = true, nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "rarity", nullable = false)
    private Rarity rarity;

    @Enumerated(EnumType.STRING)
    @Column(name = "dnd_class", nullable = false)
    private DndClass dndClass;

    // D&D 5e Base Stats
    @Column(name = "base_str", nullable = false)
    private int baseStr = 10;

    @Column(name = "base_dex", nullable = false)
    private int baseDex = 10;

    @Column(name = "base_con", nullable = false)
    private int baseCon = 10;

    @Column(name = "base_int", nullable = false)
    private int baseInt = 10;

    @Column(name = "base_wis", nullable = false)
    private int baseWis = 10;

    @Column(name = "base_cha", nullable = false)
    private int baseCha = 10;

    // New D&D Mechanical Properties
    @Column(name = "hit_die_type", nullable = false)
    private int hitDieType = 8;

    @Column(name = "primary_stat", nullable = false, length = 20)
    private String primaryStat = "STR";

    @Column(name = "class_tags", columnDefinition = "TEXT")
    private String classTags;

    @Column(name = "flavor_quips", columnDefinition = "TEXT")
    private String flavorQuips;

    @Column(name = "portrait_url")
    private String portraitUrl;
}