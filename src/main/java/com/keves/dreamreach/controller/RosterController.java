package com.keves.dreamreach.controller;

import com.keves.dreamreach.dto.CharacterRosterResponse;
import com.keves.dreamreach.service.PlayerCharacterService;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Handles incoming HTTP requests for player roster data.
 */
@RestController
@RequestMapping("/api/roster")
public class RosterController {

    private final PlayerCharacterService playerCharacterService;

    // Constructor Injection to wire the Controller to the Service
    public RosterController(PlayerCharacterService playerCharacterService) {
        this.playerCharacterService = playerCharacterService;
    }

    /**
     * Endpoint to fetch a player's full deck of characters.
     * The @PathVariable annotation pulls the name directly out of the URL.
     * Note that a whitespace needs to be treated as %20, aka John%20Doe.
     */
    @GetMapping("/{displayName}")
    public ResponseEntity<List<CharacterRosterResponse>> getRoster(@PathVariable String displayName) {
        List<CharacterRosterResponse> roster = playerCharacterService.getPlayerRoster(displayName);
        return ResponseEntity.ok(roster);
    }
}
