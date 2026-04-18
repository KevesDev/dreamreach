package com.keves.dreamreach.controller;

import com.keves.dreamreach.dto.CharacterRosterResponse;
import com.keves.dreamreach.dto.TavernListingResponse;
import com.keves.dreamreach.entity.PlayerAccount;
import com.keves.dreamreach.exception.ResourceNotFoundException;
import com.keves.dreamreach.repository.PlayerAccountRepository;
import com.keves.dreamreach.service.TavernService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tavern")
public class TavernController {

    private final TavernService tavernService;
    private final PlayerAccountRepository accountRepository;

    public TavernController(TavernService tavernService, PlayerAccountRepository accountRepository) {
        this.tavernService = tavernService;
        this.accountRepository = accountRepository;
    }

    @GetMapping
    public ResponseEntity<TavernListingResponse> getActiveListing(Authentication authentication) {
        PlayerAccount account = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        TavernListingResponse response = tavernService.getActiveListing(account.getProfile());

        // If response is null (Tavern is empty), returning HTTP 204 No Content is standard REST practice
        if (response == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/recruit")
    public ResponseEntity<?> recruitHero(Authentication authentication, @RequestParam String currencyType) {
        PlayerAccount account = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        try {
            CharacterRosterResponse response = tavernService.recruitHero(account.getProfile(), currencyType);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}