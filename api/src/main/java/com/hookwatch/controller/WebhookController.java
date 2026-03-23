package com.hookwatch.controller;

import com.hookwatch.domain.Webhook;
import com.hookwatch.dto.WebhookDto;
import com.hookwatch.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Webhook management endpoints")
public class WebhookController {

    private final WebhookService webhookService;

    @GetMapping
    @Operation(summary = "List all webhooks")
    public List<Webhook> list() {
        return webhookService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get webhook by id")
    public ResponseEntity<Webhook> get(@PathVariable UUID id) {
        return webhookService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create a new webhook")
    public Webhook create(@Valid @RequestBody WebhookDto dto) {
        Webhook webhook = Webhook.builder()
                .name(dto.getName())
                .targetUrl(dto.getTargetUrl())
                .status("ACTIVE")
                .build();
        return webhookService.save(webhook);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a webhook")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        webhookService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
