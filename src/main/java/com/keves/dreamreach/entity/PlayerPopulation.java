package com.keves.dreamreach.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;
import java.time.Instant;

@Entity
@Getter
@Setter
public class PlayerPopulation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false) private int happiness = 50;

    @Column(nullable = false) private int idlePeasants = 0;
    @Column(nullable = false) private int hunters = 0;
    @Column(nullable = false) private int woodcutters = 0;
    @Column(nullable = false) private int stoneworkers = 0;
    @Column(nullable = false) private int bakers = 0;

    // Tracks the last time the 15-minute RNG loop evaluated this population
    @Column(name = "last_population_tick", nullable = false)
    private Instant lastPopulationTick = Instant.now();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false, unique = true)
    private PlayerProfile profile;

    public int getTotalPopulation() {
        return idlePeasants + hunters + woodcutters + stoneworkers + bakers;
    }
}