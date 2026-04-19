package com.keves.dreamreach.entity;

import com.keves.dreamreach.enums.QuestType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "quest_template")
@Getter
@Setter
@NoArgsConstructor
public class QuestTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private QuestType type;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "target_stats_json", columnDefinition = "TEXT")
    private String targetStatsJson;

    @Column(name = "advantage_classes_json", columnDefinition = "TEXT")
    private String advantageClassesJson;

    @Column(name = "disadvantage_classes_json", columnDefinition = "TEXT")
    private String disadvantageClassesJson;
}