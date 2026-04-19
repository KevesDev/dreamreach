package com.keves.dreamreach.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "accepted_mission")
@Getter
@Setter
@NoArgsConstructor
public class AcceptedMission {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private PlayerProfile profile;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "quest_template_id", nullable = false)
    private QuestTemplate questTemplate;
}