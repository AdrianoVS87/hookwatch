package com.hookwatch.controller;

import com.hookwatch.domain.Agent;
import com.hookwatch.dto.AgentDto;
import com.hookwatch.dto.AgentMetricsDto;
import com.hookwatch.dto.ApiErrorDto;
import com.hookwatch.service.AgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
@Tag(name = "Agents", description = "Agent registration and metrics operations")
public class AgentController {

    private final AgentService agentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Register a new agent",
            description = "Creates an agent under a tenant and returns the persisted agent object."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Agent created"),
            @ApiResponse(responseCode = "401", description = "Missing API key", content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "403", description = "Invalid API key", content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "422", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public Agent create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Agent registration payload",
                    required = true,
                    content = @Content(examples = @ExampleObject(value = "{" +
                            "\"tenantId\":\"8bb6a6f8-e5af-4f8e-a8ba-45f4029659c8\"," +
                            "\"name\":\"OpenClaw Assistant\"," +
                            "\"description\":\"Production support bot\"}"))
            )
            @Valid @RequestBody AgentDto dto) {
        return agentService.create(dto);
    }

    @GetMapping
    @Operation(
            summary = "List agents for the authenticated tenant",
            description = "Returns all agents visible to the current API key tenant context."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agents listed"),
            @ApiResponse(responseCode = "401", description = "Missing API key", content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "403", description = "Invalid API key", content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public List<Agent> list() {
        return agentService.listForCurrentTenant();
    }

    @GetMapping("/{id}/metrics")
    @Operation(
            summary = "Get metrics for an agent",
            description = "Returns aggregate metrics such as success rate, token usage, cost, and p95 latency."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Metrics loaded"),
            @ApiResponse(responseCode = "401", description = "Missing API key", content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "403", description = "Invalid API key", content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "404", description = "Agent not found", content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public ResponseEntity<AgentMetricsDto> metrics(@PathVariable UUID id) {
        return agentService.findById(id)
                .map(a -> ResponseEntity.ok(agentService.getMetrics(id)))
                .orElse(ResponseEntity.notFound().build());
    }
}
