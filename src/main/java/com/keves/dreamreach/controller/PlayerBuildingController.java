package com.keves.dreamreach.controller;

import com.keves.dreamreach.entity.PlayerAccount;
import com.keves.dreamreach.exception.ResourceNotFoundException;
import com.keves.dreamreach.repository.PlayerAccountRepository;
import com.keves.dreamreach.service.ConstructionService;
import com.keves.dreamreach.service.EconomyService;
import com.keves.dreamreach.service.KeepLevelingService;
import com.keves.dreamreach.service.UpgradeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Handles all building assignments, removals, and structural constructions.
 */
@RestController
@RequestMapping("/api/player")
public class PlayerBuildingController {

    private final PlayerAccountRepository accountRepository;
    private final EconomyService economyService;
    private final ConstructionService constructionService;
    private final UpgradeService upgradeService;
    private final KeepLevelingService keepLevelingService;

    public PlayerBuildingController(PlayerAccountRepository accountRepository,
                                    EconomyService economyService,
                                    ConstructionService constructionService,
                                    UpgradeService upgradeService,
                                    KeepLevelingService keepLevelingService) {
        this.accountRepository = accountRepository;
        this.economyService = economyService;
        this.constructionService = constructionService;
        this.upgradeService = upgradeService;
        this.keepLevelingService = keepLevelingService;
    }

    @PostMapping("/building/assign")
    public ResponseEntity<?> assignWorker(Authentication authentication, @RequestParam String buildingId) {
        PlayerAccount account = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));
        try {
            economyService.assignWorker(account.getProfile(), UUID.fromString(buildingId));
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/building/remove")
    public ResponseEntity<?> removeWorker(Authentication authentication, @RequestParam String buildingId) {
        PlayerAccount account = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));
        try {
            economyService.removeWorker(account.getProfile(), UUID.fromString(buildingId));
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/construct")
    public ResponseEntity<?> startConstruction(Authentication authentication, @RequestParam String buildingType) {
        PlayerAccount account = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        try {
            constructionService.startConstruction(account.getProfile(), buildingType);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/construct/complete")
    public ResponseEntity<?> completeConstruction(Authentication authentication, @RequestParam String buildingType) {
        PlayerAccount account = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        try {
            constructionService.completeConstruction(account.getProfile(), buildingType);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/upgrade/start")
    public ResponseEntity<?> startUpgrade(Authentication authentication, @RequestParam String buildingId) {
        PlayerAccount account = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        try {
            upgradeService.startUpgrade(account.getProfile(), UUID.fromString(buildingId));
            return ResponseEntity.ok().build();
        } catch (IllegalStateException | IllegalArgumentException | SecurityException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/upgrade/complete")
    public ResponseEntity<?> completeUpgrade(Authentication authentication, @RequestParam String buildingId) {
        PlayerAccount account = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        try {
            upgradeService.completeUpgrade(account.getProfile(), UUID.fromString(buildingId));
            return ResponseEntity.ok().build();
        } catch (IllegalStateException | SecurityException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/keep/upgrade/start")
    public ResponseEntity<?> startKeepUpgrade(Authentication authentication) {
        PlayerAccount account = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        try {
            keepLevelingService.startKeepUpgrade(account.getProfile());
            return ResponseEntity.ok().build();
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}