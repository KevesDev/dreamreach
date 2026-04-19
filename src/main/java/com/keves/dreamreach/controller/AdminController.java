package com.keves.dreamreach.controller;

import com.keves.dreamreach.entity.PlayerAccount;
import com.keves.dreamreach.exception.ResourceNotFoundException;
import com.keves.dreamreach.repository.PlayerAccountRepository;
import com.keves.dreamreach.service.AccountCleanupService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Secure endpoints for authorized game administrators.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final PlayerAccountRepository accountRepository;
    private final AccountCleanupService cleanupService;

    public AdminController(PlayerAccountRepository accountRepository, AccountCleanupService cleanupService) {
        this.accountRepository = accountRepository;
        this.cleanupService = cleanupService;
    }

    /**
     * Allows an administrator to permanently delete any player's account and kingdom data.
     */
    @DeleteMapping("/accounts/{email}")
    public ResponseEntity<?> deleteAccount(@PathVariable String email, Authentication authentication) {
        PlayerAccount requestingAdmin = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Admin account not found."));

        if (!requestingAdmin.isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin privileges required.");
        }

        PlayerAccount targetAccount = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Target account not found."));

        cleanupService.deleteAccountAndAllRelationalData(targetAccount);
        return ResponseEntity.ok("Account " + email + " successfully deleted.");
    }
}