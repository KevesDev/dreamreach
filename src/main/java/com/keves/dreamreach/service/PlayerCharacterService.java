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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PlayerCharacterService {

    private final PlayerCharacterRepository playerCharacterRepository;
    private final PlayerProfileRepository playerProfileRepository;

    public PlayerCharacterService(PlayerCharacterRepository playerCharacterRepository, PlayerProfileRepository playerProfileRepository) {
        this.playerCharacterRepository = playerCharacterRepository;
        this.playerProfileRepository = playerProfileRepository;
    }

    @Transactional
    public List<CharacterRosterResponse> getPlayerRoster(String displayName) {
        PlayerProfile player = playerProfileRepository.findByDisplayName(displayName)
                .orElseThrow(() -> new ResourceNotFoundException("Player profile not found for display name: " + displayName));

        processPassiveRecovery(player);

        List<PlayerCharacter> rawRoster = playerCharacterRepository.findByOwnerId(player.getId());

        return rawRoster.stream()
                .map(this::mapToRosterResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void processPassiveRecovery(PlayerProfile profile) {
        List<PlayerCharacter> roster = playerCharacterRepository.findByOwnerId(profile.getId());
        Instant now = Instant.now();

        for (PlayerCharacter hero : roster) {
            boolean updated = false;

            if ("RESTING".equalsIgnoreCase(hero.getStatus()) && hero.getLongRestEndTime() != null) {
                if (now.isAfter(hero.getLongRestEndTime())) {
                    hero.setStatus("IDLE");
                    hero.setCurrentHp(hero.getMaxHp());
                    hero.setCurrentMana(hero.getMaxMana());
                    hero.setSpentHitDice(0);
                    hero.setLongRestEndTime(null);
                    hero.setLastRestTick(now);
                    updated = true;
                }
            }

            if ("IDLE".equalsIgnoreCase(hero.getStatus()) && hero.getLastRestTick() != null) {
                long hoursElapsed = ChronoUnit.HOURS.between(hero.getLastRestTick(), now);
                if (hoursElapsed > 0) {
                    for (int i = 0; i < hoursElapsed; i++) {
                        if (hero.getCurrentHp() < hero.getMaxHp() && hero.getSpentHitDice() < hero.getMaxHitDice()) {
                            hero.setSpentHitDice(hero.getSpentHitDice() + 1);

                            int avgHitDie = (hero.getTemplate().getHitDieType() / 2) + 1;
                            int conMod = DndMathUtility.calculateModifier(hero.getTotalConstitution());
                            int healAmount = Math.max(1, avgHitDie + conMod);

                            hero.setCurrentHp(Math.min(hero.getMaxHp(), hero.getCurrentHp() + healAmount));
                        }
                    }
                    hero.setLastRestTick(hero.getLastRestTick().plus(hoursElapsed, ChronoUnit.HOURS));
                    updated = true;
                }
            }

            if (updated) {
                playerCharacterRepository.save(hero);
            }
        }
    }

    @Transactional
    public void initiateLongRest(UUID characterId, String displayName) {
        PlayerProfile player = playerProfileRepository.findByDisplayName(displayName)
                .orElseThrow(() -> new ResourceNotFoundException("Player profile not found for display name: " + displayName));

        PlayerCharacter hero = playerCharacterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found."));

        if (!hero.getOwner().getId().equals(player.getId())) {
            throw new IllegalArgumentException("You do not own this character.");
        }

        if (!"IDLE".equalsIgnoreCase(hero.getStatus()) && !"KO".equalsIgnoreCase(hero.getStatus())) {
            throw new IllegalStateException("Character must be IDLE or KO to initiate a Long Rest.");
        }

        hero.setStatus("RESTING");
        hero.setLongRestEndTime(Instant.now().plus(8, ChronoUnit.HOURS));
        playerCharacterRepository.save(hero);
    }

    public CharacterRosterResponse mapToRosterResponse(PlayerCharacter character) {
        int str = character.getTotalStrength();
        int dex = character.getTotalDexterity();
        int con = character.getTotalConstitution();
        int intl = character.getTotalIntelligence();
        int wis = character.getTotalWisdom();
        int cha = character.getTotalCharisma();

        String displayRarity = character.getRolledRarity() != null
                ? character.getRolledRarity().name()
                : character.getTemplate().getRarity().name();

        return CharacterRosterResponse.builder()
                .characterId(character.getId())
                .name(character.getTemplate().getName())
                .rarity(displayRarity)
                .dndClass(character.getTemplate().getDndClass().name())
                .level(character.getCurrentLevel())
                .currentXp(character.getCurrentXp())
                .description(character.getDescription())
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
                .maxHitDice(character.getMaxHitDice())
                .manaSlotsJson(character.getManaSlotsJson())
                .status(character.getStatus())
                .lastRestTickEpoch(character.getLastRestTick() != null ? character.getLastRestTick().toEpochMilli() : null)
                .longRestEndTimeEpoch(character.getLongRestEndTime() != null ? character.getLongRestEndTime().toEpochMilli() : null) // Mapped here
                .weaponTier(character.getWeaponTier())
                .armorTier(character.getArmorTier())
                .portraitUrl(character.getTemplate().getPortraitUrl())
                .build();
    }
}