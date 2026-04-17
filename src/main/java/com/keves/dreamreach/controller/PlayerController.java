package com.keves.dreamreach.controller;

import com.keves.dreamreach.config.GameEconomyConfig;
import com.keves.dreamreach.dto.PlayerProfileResponse;
import com.keves.dreamreach.entity.PlayerAccount;
import com.keves.dreamreach.entity.PlayerProfile;
import com.keves.dreamreach.entity.PlayerPopulation;
import com.keves.dreamreach.exception.ResourceNotFoundException;
import com.keves.dreamreach.repository.PlayerAccountRepository;
import com.keves.dreamreach.service.EconomyService;
import com.keves.dreamreach.service.RewardService;
import org.springframework.web.bind.annotation.PostMapping;
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
    private final RewardService rewardService;
    private final EconomyService economyService;

    // Must match the EconomyService baseline
    private static final int BASE_WOOD_RATE = 12;
    private static final int BASE_STONE_RATE = 12;

    public PlayerController(PlayerAccountRepository accountRepository,
                            GameEconomyConfig economyConfig,
                            RewardService rewardService,
                            EconomyService economyService) {
        this.accountRepository = accountRepository;
        this.economyConfig = economyConfig;
        this.rewardService = rewardService;
        this.economyService = economyService;
    }

    /**
     * Uses the JWT token to identify the user and return their profile.
     * Spring Security automatically injects the Authentication object if the token is valid.
     * Includes real-time production rates (+/hr) and pending resources.
     */
    @GetMapping("/me")
    public ResponseEntity<PlayerProfileResponse> getMyProfile(Authentication authentication) {
        // The email was securely placed here by our JwtAuthenticationFilter
        String email = authentication.getName();

        PlayerAccount account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));


        PlayerProfile profile = account.getProfile();
        PlayerPopulation pop = profile.getPopulation();

        // Calculate dynamic limits of peasant population using the centralized ruleset
        int calculatedMaxPop = (profile.getStructures() != null)
                ? (profile.getStructures().getHouses() * economyConfig.getCapacityPerHouse())
                : 0;

        // Calculate rates including the passive baseline so the UI displays correctly
        int foodRate = economyService.calculateFoodRate(profile);
        int woodRate = (pop != null ? pop.getWoodcutters() * economyConfig.getWoodPerWoodcutter() : 0) + BASE_WOOD_RATE;
        int stoneRate = (pop != null ? pop.getStoneworkers() * economyConfig.getStonePerStoneworker() : 0) + BASE_STONE_RATE;

        // safely map main account info
        PlayerProfileResponse response = PlayerProfileResponse.builder()
                .email(account.getEmail())
                .displayName(account.getProfile().getDisplayName())
                .pvpEnabled(account.getProfile().isEffectivelyPvpEnabled())

                // Current Treasury Balances
                .food(profile.getResources() != null ? profile.getResources().getFood() : 0)
                .wood(profile.getResources() != null ? profile.getResources().getWood() : 0)
                .stone(profile.getResources() != null ? profile.getResources().getStone() : 0)
                .gold(profile.getResources() != null ? profile.getResources().getGold() : 0)
                .gems(profile.getResources() != null ? profile.getResources().getGems() : 0)

                // Production Rates (Needed for the HUD +/hr display)
                .foodRate(foodRate)
                .woodRate(woodRate)
                .stoneRate(stoneRate)

                // Pending Pool (Needed for the 'Claim' Ledger)
                .pendingFood(profile.getResources() != null ? profile.getResources().getPendingFood() : 0)
                .pendingWood(profile.getResources() != null ? profile.getResources().getPendingWood() : 0)
                .pendingStone(profile.getResources() != null ? profile.getResources().getPendingStone() : 0)

                // Safely map population metrics
                .totalPopulation(profile.getPopulation() != null ? profile.getPopulation().getTotalPopulation() : 0)
                .maxPopulation(calculatedMaxPop)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to 'Flush' pending resources into the main treasury.
     * This is called when the player clicks 'Collect All' in the UI.
     */
    @PostMapping("/claim")
    public ResponseEntity<?> claimResources(Authentication authentication) {
        PlayerAccount account = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        // This moves pending -> actual and resets the lastUpdate timestamp
        economyService.claimResources(account.getProfile());

        return ResponseEntity.ok().build();
    }

    // daily reward claim
    @PostMapping("/reward/claim")
    public ResponseEntity<?> claimDailyReward(Authentication authentication) {
        PlayerAccount account = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        try {
            // Tell the engine to process the claim
            rewardService.claimReward(account);
            return ResponseEntity.ok().build(); // 200 OK
        } catch (IllegalStateException e) {
            // If they trigger the spam protection or the reward expired, return a 400 Bad Request
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}