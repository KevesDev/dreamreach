package com.keves.dreamreach.repository;

import com.keves.dreamreach.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {

    // We will need this to look up the token when the user types it into the UI
    Optional<VerificationToken> findByCode(String code);

    // Find all tokens where the expiry date has already passed
    List<VerificationToken> findAllByExpiryDateBefore(LocalDateTime now);
}