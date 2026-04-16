package com.keves.dreamreach.controller;

import com.keves.dreamreach.dto.LoginRequest;
import com.keves.dreamreach.dto.LoginResponse;
import com.keves.dreamreach.dto.RegisterRequest;
import com.keves.dreamreach.dto.VerificationRequest;
import com.keves.dreamreach.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Entry-point manager for authentication and account management.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * The end-point for creating a new player account.
     * Uses POST to save new data.
     * @RequestBody: This tells Spring to look at the incoming JSON
     * from the React frontend and automatically "map" it into our RegisterRequest DTO.
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody RegisterRequest request) {
        authService.register(request);

        // Return a clean 201 Created status on success
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("message", "User registered successfully!"));
    }

    /**
     * The end-point for verifying a user's email address.
     * Takes the 6-digit code and unlocks the account.
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestBody VerificationRequest request) {
        authService.verifyEmail(request.getCode());

        // Return a 200 OK status response to let the frontend know the account is unlocked.
        return ResponseEntity.ok(Map.of("message", "Email verified successfully! Account is activated."));
    }

    /**
     * The end-point for authenticating a player.
     * Receives credentials, validates them, and returns a JWT.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
