package com.keves.dreamreach.controller;

import com.keves.dreamreach.config.GameEconomyConfig;
import com.keves.dreamreach.dto.PlayerProfileResponse;
import com.keves.dreamreach.entity.PlayerAccount;
import com.keves.dreamreach.entity.PlayerProfile;
import com.keves.dreamreach.exception.ResourceNotFoundException;
import com.keves.dreamreach.repository.PlayerAccountRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/player")
public class PlayerController {

    private final PlayerAccountRepository accountRepository;
    private final GameEconomyConfig economyConfig;

    public PlayerController(PlayerAccountRepository accountRepository,
                            GameEconomyConfig economyConfig) {
        this.accountRepository = accountRepository;
        this.economyConfig = economyConfig;
    }

    /**
     * Uses the JWT token to identify the user and return their profile.
     * Spring Security automatically injects the Authentication object if the token is valid.
     */
    @GetMapping("/me")
    public ResponseEntity<PlayerProfileResponse> getMyProfile(Authentication authentication) {
        // The email was securely placed here by our JwtAuthenticationFilter
        String email = authentication.getName();

        PlayerAccount account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));


        PlayerProfile profile = account.getProfile();

        // Calculate dynamic limits of peasant population using the centralized ruleset
        int calculatedMaxPop = (profile.getStructures() != null)
                ? (profile.getStructures().getHouses() * economyConfig.getCapacityPerHouse())
                : 0;

        // safely map main account info
        PlayerProfileResponse response = PlayerProfileResponse.builder()
                .email(account.getEmail())
                .displayName(account.getProfile().getDisplayName())
                .pvpEnabled(account.getProfile().isEffectivelyPvpEnabled())

        // Safely map resources
                .food(profile.getResources() != null ? profile.getResources().getFood() : 0)
                .wood(profile.getResources() != null ? profile.getResources().getWood() : 0)
                .stone(profile.getResources() != null ? profile.getResources().getStone() : 0)
                .gold(profile.getResources() != null ? profile.getResources().getGold() : 0)
                .gems(profile.getResources() != null ? profile.getResources().getGems() : 0)

                // Safely map population metrics
                .totalPopulation(profile.getPopulation() != null ? profile.getPopulation().getTotalPopulation() : 0)
                .maxPopulation(calculatedMaxPop)
                .build();

        return ResponseEntity.ok(response);
    }
}