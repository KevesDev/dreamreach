package com.keves.dreamreach.controller;

import com.keves.dreamreach.entity.CharacterTemplate;
import com.keves.dreamreach.entity.PlayerAccount;
import com.keves.dreamreach.entity.QuestTemplate;
import com.keves.dreamreach.entity.RecruitmentPool;
import com.keves.dreamreach.exception.ResourceNotFoundException;
import com.keves.dreamreach.repository.CharacterTemplateRepository;
import com.keves.dreamreach.repository.PlayerAccountRepository;
import com.keves.dreamreach.repository.QuestTemplateRepository;
import com.keves.dreamreach.repository.RecruitmentPoolRepository;
import com.keves.dreamreach.service.AccountCleanupService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    private final CharacterTemplateRepository characterTemplateRepository;
    private final RecruitmentPoolRepository recruitmentPoolRepository;

    public AdminController(PlayerAccountRepository accountRepository, AccountCleanupService cleanupService,
                           QuestTemplateRepository questTemplateRepository, CharacterTemplateRepository characterTemplateRepository,
                           RecruitmentPoolRepository recruitmentPoolRepository) {
        this.accountRepository = accountRepository;
        this.cleanupService = cleanupService;
        this.questTemplateRepository = questTemplateRepository;
        this.characterTemplateRepository = characterTemplateRepository;
        this.recruitmentPoolRepository = recruitmentPoolRepository;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminHeroDto {
        private CharacterTemplate template;
        private int tavernWeight;
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

    // --- QUESTS ---

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

    // --- HEROES / RECRUITMENT POOL ---

    @GetMapping("/heroes")
    public ResponseEntity<List<AdminHeroDto>> getAllHeroes(Authentication authentication) {
        verifyAdmin(authentication);
        List<AdminHeroDto> dtos = new ArrayList<>();
        List<CharacterTemplate> templates = characterTemplateRepository.findAll();
        for (CharacterTemplate t : templates) {
            int weight = recruitmentPoolRepository.findByCharacterTemplateId(t.getId())
                    .map(RecruitmentPool::getWeight).orElse(0);
            dtos.add(new AdminHeroDto(t, weight));
        }
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/heroes")
    public ResponseEntity<AdminHeroDto> createHero(@RequestBody AdminHeroDto dto, Authentication authentication) {
        verifyAdmin(authentication);
        CharacterTemplate savedTemplate = characterTemplateRepository.save(dto.getTemplate());
        if (dto.getTavernWeight() > 0) {
            RecruitmentPool pool = new RecruitmentPool();
            pool.setCharacterTemplate(savedTemplate);
            pool.setWeight(dto.getTavernWeight());
            recruitmentPoolRepository.save(pool);
        }
        return ResponseEntity.ok(new AdminHeroDto(savedTemplate, dto.getTavernWeight()));
    }

    @PutMapping("/heroes/{id}")
    public ResponseEntity<AdminHeroDto> updateHero(@PathVariable UUID id, @RequestBody AdminHeroDto dto, Authentication authentication) {
        verifyAdmin(authentication);
        CharacterTemplate existing = characterTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hero not found."));

        existing.setName(dto.getTemplate().getName());
        existing.setDescription(dto.getTemplate().getDescription());
        existing.setRarity(dto.getTemplate().getRarity());
        existing.setDndClass(dto.getTemplate().getDndClass());
        existing.setBaseStr(dto.getTemplate().getBaseStr());
        existing.setBaseDex(dto.getTemplate().getBaseDex());
        existing.setBaseCon(dto.getTemplate().getBaseCon());
        existing.setBaseInt(dto.getTemplate().getBaseInt());
        existing.setBaseWis(dto.getTemplate().getBaseWis());
        existing.setBaseCha(dto.getTemplate().getBaseCha());
        existing.setHitDieType(dto.getTemplate().getHitDieType());
        existing.setPrimaryStat(dto.getTemplate().getPrimaryStat());
        existing.setClassTags(dto.getTemplate().getClassTags());
        existing.setFlavorQuips(dto.getTemplate().getFlavorQuips());
        existing.setPortraitUrl(dto.getTemplate().getPortraitUrl());
        existing.setBaseGoldCost(dto.getTemplate().getBaseGoldCost());
        existing.setBaseGemCost(dto.getTemplate().getBaseGemCost());

        CharacterTemplate savedTemplate = characterTemplateRepository.save(existing);

        Optional<RecruitmentPool> existingPool = recruitmentPoolRepository.findByCharacterTemplateId(id);
        if (dto.getTavernWeight() > 0) {
            if (existingPool.isPresent()) {
                existingPool.get().setWeight(dto.getTavernWeight());
                recruitmentPoolRepository.save(existingPool.get());
            } else {
                RecruitmentPool pool = new RecruitmentPool();
                pool.setCharacterTemplate(savedTemplate);
                pool.setWeight(dto.getTavernWeight());
                recruitmentPoolRepository.save(pool);
            }
        } else {
            existingPool.ifPresent(recruitmentPoolRepository::delete);
        }

        return ResponseEntity.ok(new AdminHeroDto(savedTemplate, dto.getTavernWeight()));
    }

    @DeleteMapping("/heroes/{id}")
    public ResponseEntity<?> deleteHero(@PathVariable UUID id, Authentication authentication) {
        verifyAdmin(authentication);
        try {
            recruitmentPoolRepository.findByCharacterTemplateId(id).ifPresent(recruitmentPoolRepository::delete);
            characterTemplateRepository.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Cannot hard-delete this hero. Players might already own them in their roster.");
        }
    }
}