package com.keves.dreamreach.controller;

import com.keves.dreamreach.dto.PartyCalculateRequest;
import com.keves.dreamreach.dto.PartyCalculateResponse;
import com.keves.dreamreach.dto.PartySaveRequest;
import com.keves.dreamreach.entity.PlayerAccount;
import com.keves.dreamreach.entity.QuestTemplate;
import com.keves.dreamreach.exception.ResourceNotFoundException;
import com.keves.dreamreach.repository.PlayerAccountRepository;
import com.keves.dreamreach.service.MissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/missions")
public class MissionController {

    private final MissionService missionService;
    private final PlayerAccountRepository accountRepository;

    public MissionController(MissionService missionService, PlayerAccountRepository accountRepository) {
        this.missionService = missionService;
        this.accountRepository = accountRepository;
    }

    @GetMapping("/quests")
    public ResponseEntity<List<QuestTemplate>> getAllQuests() {
        return ResponseEntity.ok(missionService.getAllQuests());
    }

    @PostMapping("/party/calculate")
    public ResponseEntity<PartyCalculateResponse> calculate(@RequestBody PartyCalculateRequest request) {
        int chance = missionService.calculateSuccessChance(request.getCharacterIds(), request.getQuestId());
        return ResponseEntity.ok(new PartyCalculateResponse(chance));
    }

    @PostMapping("/party/save")
    public ResponseEntity<?> saveParty(@RequestBody PartySaveRequest request, Authentication authentication) {
        PlayerAccount account = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        missionService.saveParty(account.getProfile().getDisplayName(), request.getCharacterIds());
        return ResponseEntity.ok().build();
    }
}