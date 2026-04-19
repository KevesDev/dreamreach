package com.keves.dreamreach.entity;

import com.keves.dreamreach.enums.DndClass;
import com.keves.dreamreach.enums.Rarity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

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

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "rarity", nullable = false)
    private Rarity rarity;

    @Enumerated(EnumType.STRING)
    @Column(name = "dnd_class", nullable = false)
    private DndClass dndClass;

    @Column(name = "base_str") private int baseStr;
    @Column(name = "base_dex") private int baseDex;
    @Column(name = "base_con") private int baseCon;
    @Column(name = "base_int") private int baseInt;
    @Column(name = "base_wis") private int baseWis;
    @Column(name = "base_cha") private int baseCha;

    @Column(name = "hit_die_type") private int hitDieType;
    @Column(name = "primary_stat") private String primaryStat;

    @Column(name = "class_tags") private String classTags; // JSON array of tags
    @Column(name = "flavor_quips") private String flavorQuips; // JSON object: {"IDLE": "...", "MISSION": "..."}
    @Column(name = "portrait_url") private String portraitUrl;

    @Column(name = "base_gold_cost") private int baseGoldCost;
    @Column(name = "base_gem_cost") private int baseGemCost;

    @Column(name = "stat_priority_json", columnDefinition = "TEXT")
    private String statPriorityJson = "[\"STR\", \"CON\", \"DEX\", \"WIS\", \"CHA\", \"INT\"]";
}