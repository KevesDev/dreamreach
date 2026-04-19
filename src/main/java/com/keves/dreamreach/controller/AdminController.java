package com.keves.dreamreach.controller;

import com.keves.dreamreach.entity.PlayerAccount;
import com.keves.dreamreach.entity.QuestTemplate;
import com.keves.dreamreach.exception.ResourceNotFoundException;
import com.keves.dreamreach.repository.PlayerAccountRepository;
import com.keves.dreamreach.repository.QuestTemplateRepository;
import com.keves.dreamreach.service.AccountCleanupService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Secure endpoints for authorized game administrators.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final PlayerAccountRepository accountRepository;
    private final AccountCleanupService cleanupService;
    private final QuestTemplateRepository questTemplateRepository;

    public AdminController(PlayerAccountRepository accountRepository, AccountCleanupService cleanupService, QuestTemplateRepository questTemplateRepository) {
        this.accountRepository = accountRepository;
        this.cleanupService = cleanupService;
        this.questTemplateRepository = questTemplateRepository;
    }

    private void verifyAdmin(Authentication authentication) {
        PlayerAccount requestingAdmin = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Admin account not found."));
        if (!requestingAdmin.isAdmin()) {
            throw new SecurityException("Admin privileges required.");
        }
    }

    @DeleteMapping("/accounts/{email}")
    public ResponseEntity<?> deleteAccount(@PathVariable String email, Authentication authentication) {
        try {
            verifyAdmin(authentication);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }

        PlayerAccount targetAccount = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Target account not found."));

        cleanupService.deleteAccountAndAllRelationalData(targetAccount);
        return ResponseEntity.ok("Account " + email + " successfully deleted.");
    }

    @GetMapping("/quests")
    public ResponseEntity<List<QuestTemplate>> getAllQuests(Authentication authentication) {
        verifyAdmin(authentication);
        return ResponseEntity.ok(questTemplateRepository.findAll());
    }

    @PostMapping("/quests")
    public ResponseEntity<QuestTemplate> createQuest(@RequestBody QuestTemplate quest, Authentication authentication) {
        verifyAdmin(authentication);
        return ResponseEntity.ok(questTemplateRepository.save(quest));
    }

    @PutMapping("/quests/{id}")
    public ResponseEntity<QuestTemplate> updateQuest(@PathVariable UUID id, @RequestBody QuestTemplate questDetails, Authentication authentication) {
        verifyAdmin(authentication);
        QuestTemplate existing = questTemplateRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Quest not found."));

        existing.setType(questDetails.getType());
        existing.setTitle(questDetails.getTitle());
        existing.setDescription(questDetails.getDescription());
        existing.setTargetStatsJson(questDetails.getTargetStatsJson());
        existing.setAdvantageClassesJson(questDetails.getAdvantageClassesJson());
        existing.setDisadvantageClassesJson(questDetails.getDisadvantageClassesJson());
        existing.setBaseExp(questDetails.getBaseExp());
        existing.setRewardGold(questDetails.getRewardGold());
        existing.setRewardGems(questDetails.getRewardGems());
        existing.setRewardFood(questDetails.getRewardFood());
        existing.setRewardWood(questDetails.getRewardWood());
        existing.setRewardStone(questDetails.getRewardStone());
        existing.setDurationHours(questDetails.getDurationHours());
        existing.setPublished(questDetails.isPublished());

        return ResponseEntity.ok(questTemplateRepository.save(existing));
    }

    @DeleteMapping("/quests/{id}")
    public ResponseEntity<?> deleteQuest(@PathVariable UUID id, Authentication authentication) {
        verifyAdmin(authentication);
        try {
            questTemplateRepository.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Cannot hard-delete this quest. It is actively referenced by player journals or completed histories. Please 'Unpublish' it from the board instead.");
        }
    }
}