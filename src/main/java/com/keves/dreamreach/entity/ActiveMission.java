package com.keves.dreamreach.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "active_mission")
@Getter
@Setter
@NoArgsConstructor
public class ActiveMission {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "quest_template_id", nullable = false)
    private QuestTemplate questTemplate;

    @Column(name = "success_chance", nullable = false)
    private int successChance;

    @Column(name = "dispatch_time", nullable = false)
    private Instant dispatchTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;
}