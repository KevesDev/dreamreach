package com.keves.dreamreach.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents an alliance created and run by players.
 * This defines any overarching rules for members, like
 * forced pvp flagging.
 */
@Entity
@Table(name = "alliance")
@Getter
@Setter
@NoArgsConstructor
public class Alliance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", unique = true, nullable = false, length = 100)
    private String name;

    @Column(name = "tag", unique = true, nullable = false, length = 10)
    private String tag;

    @Column(name = "level", nullable = false, length = 10)
    private int level;

    @Column(name = "description", nullable = true, length = 255)
    private String description;

    @Column(name = "is_alliance_pvp_enabled", nullable = false)
    private boolean isAlliancePvpEnabled = false;

    /**
     * Bidirectional relationship mapping members to this specific alliance.
     * Mapped by the 'alliance' field on the PlayerProfile entity.
     */
    @OneToMany(mappedBy = "alliance")
    private List<PlayerProfile> members = new ArrayList<>();
}