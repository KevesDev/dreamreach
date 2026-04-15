package com.keves.dreamreach.repository;

import com.keves.dreamreach.entity.PlayerCharacter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * DAO for the PlayerCharacter entity.
 */
@Repository
public interface PlayerCharacterRepository extends JpaRepository<PlayerCharacter, UUID> {

    // Return a list of player characters by owner ID.
    List<PlayerCharacter> findByOwnerId(UUID ownerId);
}
