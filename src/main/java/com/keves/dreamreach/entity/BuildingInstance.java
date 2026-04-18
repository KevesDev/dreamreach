package com.keves.dreamreach.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "building_instances")
@Getter
@Setter
@NoArgsConstructor
public class BuildingInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private PlayerProfile profile;

    @Column(name = "building_type", nullable = false, length = 50)
    private String buildingType;

    @Column(name = "level", nullable = false)
    private int level = 1;

    @Column(name = "assigned_workers", nullable = false)
    private int assignedWorkers = 0;
}