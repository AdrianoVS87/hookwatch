package com.hookwatch.controller;

import com.hookwatch.domain.Agent;
import com.hookwatch.dto.AgentDto;
import com.hookwatch.dto.AgentMetricsDto;
import com.hookwatch.service.AgentService;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "Agents")
public class AgentController {

    private final AgentService agentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new agent")
    public Agent create(@Valid @RequestBody AgentDto dto) {
        return agentService.create(dto);
    }

    @GetMapping
    @Operation(summary = "List all agents for the authenticated tenant")
    public List<Agent> list() {
        return agentService.listForCurrentTenant();
    }

    @GetMapping("/{id}/metrics")
    @Operation(summary = "Get metrics for an agent")
    public ResponseEntity<AgentMetricsDto> metrics(@PathVariable UUID id) {
        return agentService.findById(id)
                .map(a -> ResponseEntity.ok(agentService.getMetrics(id)))
                .orElse(ResponseEntity.notFound().build());
    }
}
