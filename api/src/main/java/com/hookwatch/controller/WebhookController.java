package com.hookwatch.controller;

import com.hookwatch.domain.Webhook;
import com.hookwatch.dto.ApiErrorDto;
import com.hookwatch.dto.WebhookDto;
import com.hookwatch.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Tag(name = "System", description = "Webhook management and system integrations")
public class WebhookController {

    private final WebhookService webhookService;

    @GetMapping
    @Operation(summary = "List webhooks", description = "Returns all configured webhooks.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Webhooks listed"),
            @ApiResponse(responseCode = "401", description = "Missing API key", content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "403", description = "Invalid API key", content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public List<Webhook> list() {
        return webhookService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get webhook by id", description = "Returns a webhook configuration by id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Webhook found"),
            @ApiResponse(responseCode = "401", description = "Missing API key", content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "403", description = "Invalid API key", content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "404", description = "Webhook not found", content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public ResponseEntity<Webhook> get(@PathVariable UUID id) {
        return webhookService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create webhook", description = "Creates a new webhook target in ACTIVE state.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Webhook created"),
            @ApiResponse(responseCode = "401", description = "Missing API key", content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "403", description = "Invalid API key", content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "422", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public Webhook create(@Valid @RequestBody WebhookDto dto) {
        Webhook webhook = Webhook.builder()
                .name(dto.getName())
                .targetUrl(dto.getTargetUrl())
                .status("ACTIVE")
                .build();
        return webhookService.save(webhook);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete webhook", description = "Removes a webhook by id.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Webhook deleted"),
            @ApiResponse(responseCode = "401", description = "Missing API key", content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "403", description = "Invalid API key", content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "404", description = "Webhook not found", content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        webhookService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
