package com.keves.dreamreach.controller;

import com.keves.dreamreach.dto.*;
import com.keves.dreamreach.entity.PlayerAccount;
import com.keves.dreamreach.entity.QuestTemplate;
import com.keves.dreamreach.exception.ResourceNotFoundException;
import com.keves.dreamreach.repository.PlayerAccountRepository;
import com.keves.dreamreach.service.MissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/missions")
public class MissionController {

    private final MissionService missionService;
    private final PlayerAccountRepository accountRepository;

    public MissionController(MissionService missionService, PlayerAccountRepository accountRepository) {
        this.missionService = missionService;
        this.accountRepository = accountRepository;
    }

    @GetMapping("/board")
    public ResponseEntity<List<QuestTemplate>> getBoard(Authentication authentication) {
        PlayerAccount account = accountRepository.findByEmail(authentication.getName()).orElseThrow(() -> new ResourceNotFoundException("Account not found."));
        return ResponseEntity.ok(missionService.getAdventurersBoard(account.getProfile().getDisplayName()));
    }

    @PostMapping("/accept/{questId}")
    public ResponseEntity<?> acceptMission(@PathVariable UUID questId, Authentication authentication) {
        PlayerAccount account = accountRepository.findByEmail(authentication.getName()).orElseThrow(() -> new ResourceNotFoundException("Account not found."));
        missionService.acceptMission(account.getProfile().getDisplayName(), questId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/journal")
    public ResponseEntity<List<QuestTemplate>> getJournal(Authentication authentication) {
        PlayerAccount account = accountRepository.findByEmail(authentication.getName()).orElseThrow(() -> new ResourceNotFoundException("Account not found."));
        return ResponseEntity.ok(missionService.getJournal(account.getProfile().getDisplayName()));
    }

    @PostMapping("/party/calculate")
    public ResponseEntity<PartyCalculateResponse> calculate(@RequestBody PartyCalculateRequest request) {
        int chance = missionService.calculateSuccessChance(request.getCharacterIds(), request.getQuestId());
        return ResponseEntity.ok(new PartyCalculateResponse(chance));
    }

    @PostMapping("/dispatch")
    public ResponseEntity<?> dispatchParty(@RequestBody MissionDispatchRequest request, Authentication authentication) {
        PlayerAccount account = accountRepository.findByEmail(authentication.getName()).orElseThrow(() -> new ResourceNotFoundException("Account not found."));
        missionService.dispatchParty(account.getProfile().getDisplayName(), request.getQuestId(), request.getCharacterIds());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/active")
    public ResponseEntity<List<ActiveMissionResponse>> getActiveMissions(Authentication authentication) {
        PlayerAccount account = accountRepository.findByEmail(authentication.getName()).orElseThrow(() -> new ResourceNotFoundException("Account not found."));
        return ResponseEntity.ok(missionService.getActiveMissions(account.getProfile().getDisplayName()));
    }
}