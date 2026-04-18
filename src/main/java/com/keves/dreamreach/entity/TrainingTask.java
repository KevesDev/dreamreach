package com.keves.dreamreach.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "training_tasks")
@Getter
@Setter
@NoArgsConstructor
public class TrainingTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private PlayerProfile profile;

    @Column(name = "profession_type", nullable = false, length = 50)
    private String professionType;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "completion_time", nullable = false)
    private Instant completionTime;
}