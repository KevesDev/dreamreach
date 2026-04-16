package com.keves.dreamreach.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * The secure hidden class for storing player
 * login credentials. kept separate from player profile.
 */

@Entity
@Getter
@Setter
public class PlayerAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    //// This holds the BCrypt hash
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    boolean isEnabled = false; // this gets toggled when user verifies email for a new account.

    @Column(name = "last_login_date")
    private Instant lastLoginDate;

    @Column(name = "consecutive_logins", nullable = false)
    private int consecutiveLogins = 0;

    @Column(name = "last_claim_date")
    private Instant lastClaimDate;

    @Embedded
    private PendingReward pendingReward;

    /**
     * mappedBy = "account": This tells Spring that the 'PlayerProfile'
     * entity is the one that physically holds the foreign key column.
     * * CascadeType.ALL: If we delete an account, the profile is
     * automatically deleted. No "orphaned" data.
     */
    @OneToOne(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private PlayerProfile profile;
}
