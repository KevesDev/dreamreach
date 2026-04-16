package com.keves.dreamreach.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Entity
@Getter
@Setter
public class PlayerResources {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false) private int food = 0;
    @Column(nullable = false) private int wood = 0;
    @Column(nullable = false) private int stone = 0;
    @Column(nullable = false) private int gold = 0;
    @Column(nullable = false) private int gems = 0;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false, unique = true)
    private PlayerProfile profile;
}