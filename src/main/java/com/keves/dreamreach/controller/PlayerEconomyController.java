package com.keves.dreamreach.controller;

import com.keves.dreamreach.entity.PlayerAccount;
import com.keves.dreamreach.exception.ResourceNotFoundException;
import com.keves.dreamreach.repository.PlayerAccountRepository;
import com.keves.dreamreach.service.EconomyService;
import com.keves.dreamreach.service.RewardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles treasury transactions, tax rates, and daily claims.
 */
@RestController
@RequestMapping("/api/player")
public class PlayerEconomyController {

    private final PlayerAccountRepository accountRepository;
    private final EconomyService economyService;
    private final RewardService rewardService;

    public PlayerEconomyController(PlayerAccountRepository accountRepository,
                                   EconomyService economyService,
                                   RewardService rewardService) {
        this.accountRepository = accountRepository;
        this.economyService = economyService;
        this.rewardService = rewardService;
    }

    @PostMapping("/claim")
    public ResponseEntity<?> claimResources(Authentication authentication) {
        PlayerAccount account = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        economyService.claimResources(account.getProfile());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reward/claim")
    public ResponseEntity<?> claimDailyReward(Authentication authentication) {
        PlayerAccount account = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        try {
            rewardService.claimReward(account);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/taxes/bracket")
    public ResponseEntity<?> setTaxBracket(Authentication authentication, @RequestParam String bracket) {
        PlayerAccount account = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        try {
            economyService.setTaxBracket(account.getProfile(), bracket);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/taxes/collect")
    public ResponseEntity<?> collectTaxes(Authentication authentication) {
        PlayerAccount account = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        try {
            economyService.collectTaxes(account.getProfile());
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}