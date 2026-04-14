package com.keves.dreamreach.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles system-level diagnostic endpoints.
 */
@RestController
@RequestMapping("/api/system")
public class HealthController {

    /**
     * Verifies the application context is loaded and responding to http requests.
     * @return a status confirmation string.
     */
    @GetMapping("/health")
    public String checkHealth() {
        return ("DreamReach API is online and healthy!");
    }
}
