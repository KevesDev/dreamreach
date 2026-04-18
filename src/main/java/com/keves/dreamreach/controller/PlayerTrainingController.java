package com.keves.dreamreach.controller;

import com.keves.dreamreach.entity.PlayerAccount;
import com.keves.dreamreach.exception.ResourceNotFoundException;
import com.keves.dreamreach.repository.PlayerAccountRepository;
import com.keves.dreamreach.service.TrainingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles the training queues for converting idle peasants into professionals.
 */
@RestController
@RequestMapping("/api/player")
public class PlayerTrainingController {

    private final PlayerAccountRepository accountRepository;
    private final TrainingService trainingService;

    public PlayerTrainingController(PlayerAccountRepository accountRepository, TrainingService trainingService) {
        this.accountRepository = accountRepository;
        this.trainingService = trainingService;
    }

    @PostMapping("/train")
    public ResponseEntity<?> queueTraining(Authentication authentication,
                                           @RequestParam String profession,
                                           @RequestParam int quantity) {
        PlayerAccount account = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        try {
            trainingService.queueTraining(account.getProfile(), profession, quantity);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/train/complete")
    public ResponseEntity<?> completeTraining(Authentication authentication, @RequestParam String taskId) {
        PlayerAccount account = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        try {
            trainingService.completeTraining(account.getProfile(), taskId);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}