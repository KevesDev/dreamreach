package com.keves.dreamreach.repository;

import com.keves.dreamreach.entity.CharacterTemplate;
import com.keves.dreamreach.enums.Rarity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data Access Object (DAO) for the CharacterTemplate entity.
 * If we need to query a static dictionary of characters, we
 * use this to pull the list.
 */
@Repository
public interface CharacterTemplateRepository extends JpaRepository<CharacterTemplate, UUID> {
    Optional<CharacterTemplate> findByName(String name);
    List<CharacterTemplate> findByRarity(Rarity rarity);

    /**
     * We do not have to write the implementation for findByRarity.
     * Spring looks at the method signature, sees the word "Rarity"
     * which matches the column in our entity, and automatically
     * generates the optimized SELECT SQL statement under the hood.
     */
}
