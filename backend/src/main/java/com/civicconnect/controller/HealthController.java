package com.civicconnect.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Simple health check endpoint for CivicConnect.
 *
 * <p>Provides a lightweight {@code GET /api/health} endpoint that returns
 * basic application status without requiring actuator authentication.
 * The actuator {@code /actuator/health} endpoint remains available for
 * infrastructure monitoring with more detail.
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "civicconnect-backend",
                "timestamp", OffsetDateTime.now().toString()
        );
    }
}