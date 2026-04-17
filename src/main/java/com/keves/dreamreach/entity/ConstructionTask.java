package com.keves.dreamreach.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "construction_tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConstructionTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Many tasks can belong to one profile.
    // FetchType.LAZY is a performance optimization. If we load a task, we don't
    // want Hibernate to automatically do a massive JOIN to load the entire PlayerProfile
    // unless we specifically ask for it.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private PlayerProfile profile;

    @Column(name = "building_type", nullable = false)
    private String buildingType; // e.g., "bakery", "lodge", "house"

    @Column(name = "target_level", nullable = false)
    private int targetLevel;

    // We use Instant because servers operate in UTC. It avoids time-zone bugs
    // if a player in Idaho plays with a player in London.
    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "completion_time", nullable = false)
    private Instant completionTime;
}