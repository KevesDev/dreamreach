package com.keves.dreamreach.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "upgrade_task")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpgradeTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private PlayerProfile profile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_instance_id", nullable = false)
    private BuildingInstance buildingInstance;

    @Column(name = "target_level", nullable = false)
    private Integer targetLevel;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "completion_time", nullable = false)
    private Instant completionTime;
}