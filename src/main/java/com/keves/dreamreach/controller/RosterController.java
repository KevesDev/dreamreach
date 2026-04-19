package com.keves.dreamreach.controller;

import com.keves.dreamreach.dto.CharacterRosterResponse;
import com.keves.dreamreach.entity.PlayerAccount;
import com.keves.dreamreach.exception.ResourceNotFoundException;
import com.keves.dreamreach.repository.PlayerAccountRepository;
import com.keves.dreamreach.service.PlayerCharacterService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Handles incoming HTTP requests for player roster data.
 */
@RestController
@RequestMapping("/api/roster")
public class RosterController {

    private final PlayerCharacterService playerCharacterService;
    private final PlayerAccountRepository accountRepository;

    // Constructor Injection to wire the Controller to the Services
    public RosterController(PlayerCharacterService playerCharacterService, PlayerAccountRepository accountRepository) {
        this.playerCharacterService = playerCharacterService;
        this.accountRepository = accountRepository;
    }

    /**
     * Endpoint to fetch the authenticated player's full deck of characters.
     * Securely uses the JWT token to identify the player rather than a URL parameter.
     */
    @GetMapping
    public ResponseEntity<List<CharacterRosterResponse>> getMyRoster(Authentication authentication) {
        // Extract the user's email from the secure JWT token
        PlayerAccount account = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        // Fetch the roster using the display name tied to their authenticated profile
        // This implicitly calculates all passive short/long rest recovery logic
        List<CharacterRosterResponse> roster = playerCharacterService.getPlayerRoster(account.getProfile().getDisplayName());

        return ResponseEntity.ok(roster);
    }

    /**
     * Initiates a Long Rest for a specific character.
     */
    @PostMapping("/{characterId}/rest")
    public ResponseEntity<?> initiateLongRest(@PathVariable UUID characterId, Authentication authentication) {
        PlayerAccount account = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        playerCharacterService.initiateLongRest(characterId, account.getProfile().getDisplayName());
        return ResponseEntity.ok().build();
    }
}