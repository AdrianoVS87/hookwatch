package com.hookwatch.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "System", description = "System and operational endpoints")
public class HealthController {

    @GetMapping("/api/v1/health")
    @Operation(
            summary = "Health check",
            description = "Simple liveness endpoint used by deployments and probes."
    )
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "hookwatch-api"));
    }
}
