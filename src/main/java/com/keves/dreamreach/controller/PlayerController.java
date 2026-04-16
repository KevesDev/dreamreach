package com.keves.dreamreach.controller;

import com.keves.dreamreach.dto.PlayerProfileResponse;
import com.keves.dreamreach.entity.PlayerAccount;
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

    public PlayerController(PlayerAccountRepository accountRepository) {
        this.accountRepository = accountRepository;
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

        PlayerProfileResponse response = PlayerProfileResponse.builder()
                .email(account.getEmail())
                .displayName(account.getProfile().getDisplayName())
                .pvpEnabled(account.getProfile().isEffectivelyPvpEnabled())
                .build();

        return ResponseEntity.ok(response);
    }
}