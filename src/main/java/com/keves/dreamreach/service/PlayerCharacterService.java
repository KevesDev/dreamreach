package com.keves.dreamreach.service;

import com.keves.dreamreach.dto.CharacterRosterResponse;
import com.keves.dreamreach.entity.PlayerCharacter;
import com.keves.dreamreach.entity.PlayerProfile;
import com.keves.dreamreach.exception.ResourceNotFoundException;
import com.keves.dreamreach.repository.PlayerCharacterRepository;
import com.keves.dreamreach.repository.PlayerProfileRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * asks the repository for the database entities,
 * then translates them into our safe DTOs.
 */
@Service
public class PlayerCharacterService {

    private final PlayerCharacterRepository playerCharacterRepository;
    private final PlayerProfileRepository playerProfileRepository;

    public PlayerCharacterService(PlayerCharacterRepository playerCharacterRepository, PlayerProfileRepository playerProfileRepository) {
        this.playerCharacterRepository = playerCharacterRepository;
        this.playerProfileRepository = playerProfileRepository;
    }

    /**
     * Retrieves a player's entire roster and maps it to safe data transfer objects.
     * Generally useful for UX exposure (and avoids stackOverflow infinite loop from db associations).
     * @param displayName The unique display name of the player.
     * @return A list of mapped CharacterRosterResponse objects.
     * @throws ResourceNotFoundException if the player profile does not exist.
     */
    @Transactional(readOnly = true)
    public List<CharacterRosterResponse> getPlayerRoster(String displayName) {

        /**Find the player by their display name.
         * Does not return a PlayerProfile directly. It returns an Optional<PlayerProfile>.
         * 'optiona' is like a box that may or may not be empty.
         * For the .orElseThrow(), This is how we open the box safely. It says:
         * "If the player is in the box, give it to me. If the box is empty,
         * throw this specific error immediately so we don't crash with a NullPointerException later."
         */
        PlayerProfile player = playerProfileRepository.findByDisplayName(displayName)
                .orElseThrow(() -> new ResourceNotFoundException("Player profile not found for display name: " + displayName));

        // Once we have the player, we grab their raw deck of characters from the database (rawRoster).
        List<PlayerCharacter> rawRoster = playerCharacterRepository.findByOwnerId(player.getId());

        /**
         * Stream API Data Transformation (The Assembly Line):
         * 1. .stream()  -> Places the raw database entities onto a conveyor belt.
         * 2. .map()     -> Transforms each passing entity into a safe DTO using the helper method below.
         * 3. .collect() -> Gathers all the newly transformed DTOs and packs them into a new List.
         */
        return rawRoster.stream()
                .map(this::mapToRosterResponse)
                .collect(Collectors.toList());
    }

    /**
     * Maps a persistent PlayerCharacter entity to a safe data transfer object.
     * It takes the heavy, database-linked PlayerCharacter and extracts only the
     * flat data we want the web browser to see.
     */
    private CharacterRosterResponse mapToRosterResponse(PlayerCharacter character) {
        return CharacterRosterResponse.builder()
                .characterId(character.getId())
                .name(character.getTemplate().getName())
                .rarity(character.getTemplate().getRarity().name())
                .dndClass(character.getTemplate().getDndClass().name())
                .level(character.getCurrentLevel())
                .currentXp(character.getCurrentXp())
                .totalStrength(character.getTotalStrength())
                .totalDexterity(character.getTotalDexterity())
                .totalConstitution(character.getTotalConstitution())
                .totalIntelligence(character.getTotalIntelligence())
                .totalWisdom(character.getTotalWisdom())
                .totalCharisma(character.getTotalCharisma())
                .build();
        /**
         * Because we added the @Builder annotation from Lombok onto our DTO class, we do not
         * have to use a massive constructor like new CharacterRosterResponse(id, name, rarity, level...).
         * A Builder lets you set the variables by name, one by one, and then call .build()
         * at the end to snap it all together. It is much cleaner and prevents you from accidentally
         * putting the "level" integer into the "currentXp" slot.
         */
    }
}
