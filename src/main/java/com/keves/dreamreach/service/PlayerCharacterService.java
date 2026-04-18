package com.keves.dreamreach.service;

import com.keves.dreamreach.dto.CharacterRosterResponse;
import com.keves.dreamreach.entity.PlayerCharacter;
import com.keves.dreamreach.entity.PlayerProfile;
import com.keves.dreamreach.exception.ResourceNotFoundException;
import com.keves.dreamreach.repository.PlayerCharacterRepository;
import com.keves.dreamreach.repository.PlayerProfileRepository;
import com.keves.dreamreach.util.DndMathUtility;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlayerCharacterService {

    private final PlayerCharacterRepository playerCharacterRepository;
    private final PlayerProfileRepository playerProfileRepository;

    public PlayerCharacterService(PlayerCharacterRepository playerCharacterRepository, PlayerProfileRepository playerProfileRepository) {
        this.playerCharacterRepository = playerCharacterRepository;
        this.playerProfileRepository = playerProfileRepository;
    }

    @Transactional(readOnly = true)
    public List<CharacterRosterResponse> getPlayerRoster(String displayName) {
        PlayerProfile player = playerProfileRepository.findByDisplayName(displayName)
                .orElseThrow(() -> new ResourceNotFoundException("Player profile not found for display name: " + displayName));

        List<PlayerCharacter> rawRoster = playerCharacterRepository.findByOwnerId(player.getId());

        return rawRoster.stream()
                .map(this::mapToRosterResponse)
                .collect(Collectors.toList());
    }

    private CharacterRosterResponse mapToRosterResponse(PlayerCharacter character) {
        int str = character.getTotalStrength();
        int dex = character.getTotalDexterity();
        int con = character.getTotalConstitution();
        int intl = character.getTotalIntelligence();
        int wis = character.getTotalWisdom();
        int cha = character.getTotalCharisma();

        return CharacterRosterResponse.builder()
                .characterId(character.getId())
                .name(character.getTemplate().getName())
                .rarity(character.getTemplate().getRarity().name())
                .dndClass(character.getTemplate().getDndClass().name())
                .level(character.getCurrentLevel())
                .currentXp(character.getCurrentXp())
                .totalStrength(str)
                .totalDexterity(dex)
                .totalConstitution(con)
                .totalIntelligence(intl)
                .totalWisdom(wis)
                .totalCharisma(cha)
                .strMod(DndMathUtility.calculateModifier(str))
                .dexMod(DndMathUtility.calculateModifier(dex))
                .conMod(DndMathUtility.calculateModifier(con))
                .intMod(DndMathUtility.calculateModifier(intl))
                .wisMod(DndMathUtility.calculateModifier(wis))
                .chaMod(DndMathUtility.calculateModifier(cha))
                .currentHp(character.getCurrentHp())
                .maxHp(character.getMaxHp())
                .spentHitDice(character.getSpentHitDice())
                .status(character.getStatus())
                .weaponTier(character.getWeaponTier())
                .armorTier(character.getArmorTier())
                .portraitUrl(character.getTemplate().getPortraitUrl())
                .build();
    }
}