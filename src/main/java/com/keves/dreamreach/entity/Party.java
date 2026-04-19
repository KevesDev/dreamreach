package com.keves.dreamreach.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "party")
@Getter
@Setter
@NoArgsConstructor
public class Party {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private PlayerProfile owner;

    @Column(name = "slot1_id") private UUID slot1Id;
    @Column(name = "slot2_id") private UUID slot2Id;
    @Column(name = "slot3_id") private UUID slot3Id;
    @Column(name = "slot4_id") private UUID slot4Id;
    @Column(name = "slot5_id") private UUID slot5Id;
}