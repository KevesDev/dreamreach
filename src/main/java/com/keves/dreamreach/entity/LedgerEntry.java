package com.keves.dreamreach.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entry")
@Getter
@Setter
@NoArgsConstructor
public class LedgerEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private PlayerProfile profile;

    @Column(name = "event_timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;
}