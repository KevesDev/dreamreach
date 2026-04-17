package com.keves.dreamreach.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Entity
@Getter
@Setter
public class PlayerStructures {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false) private int houses = 0;
    @Column(nullable = false) private int towers = 0;
    @Column(nullable = false) private int bakeries = 0;

    // THE ONLY NEEDED ADDITION
    @Column(name = "hunting_lodges", nullable = false) private int huntingLodges = 0;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false, unique = true)
    private PlayerProfile profile;
}