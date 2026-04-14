package com.keves.dreamreach.repository;

import com.keves.dreamreach.entity.PlayerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * The DAO (Data Access Object) for the PlayerProfile entity.
 * Spring automatically implements the standard CRUD operations at runtime.
 */
@Repository
public interface PlayerProfileRepository extends JpaRepository<PlayerProfile, UUID> {
    // Translates to: SELECT * FROM PlayerProfile WHERE display_name = ?
    Optional<PlayerProfile> findByDisplayName(String displayName);

}
