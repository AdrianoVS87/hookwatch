package com.hookwatch.controller;

import com.hookwatch.domain.Agent;
import com.hookwatch.domain.Span;
import com.hookwatch.domain.Trace;
import com.hookwatch.dto.OtelExportDto;
import com.hookwatch.repository.AgentRepository;
import com.hookwatch.repository.TraceRepository;
import com.hookwatch.security.TenantContext;
import com.hookwatch.service.OtelMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "OpenTelemetry")
public class OtelController {

    private final TraceRepository traceRepository;
    private final AgentRepository agentRepository;
    private final OtelMapper otelMapper;

    /**
     * Export a HookWatch trace in OTLP JSON format.
     * GET /api/v1/traces/{id}/otel
     */
    @GetMapping("/api/v1/traces/{id}/otel")
    @Operation(summary = "Export a trace in OTLP JSON format")
    public ResponseEntity<OtelExportDto> exportOtel(@PathVariable UUID id) {
        UUID tenantId = TenantContext.get();

        Trace trace;
        if (tenantId != null) {
            trace = traceRepository.findByIdAndTenantId(id, tenantId)
                    .orElseThrow(() -> {
                        // Check if trace exists at all to distinguish 404 vs 403
                        if (traceRepository.existsById(id)) {
                            return new ResponseStatusException(HttpStatus.FORBIDDEN,
                                    "Access denied: trace does not belong to your tenant");
                        }
                        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Trace not found");
                    });
        } else {
            trace = traceRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trace not found"));
        }

        Agent agent = agentRepository.findById(trace.getAgentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Agent not found for trace"));

        OtelExportDto dto = otelMapper.toOtel(trace, agent);
        return ResponseEntity.ok(dto);
    }

    /**
     * Ingest a trace from OTLP JSON format.
     * POST /api/v1/ingest/otel
     * Returns the created trace ID plus span count.
     */
    @PostMapping("/api/v1/ingest/otel")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Ingest a trace from OTLP JSON format")
    public ResponseEntity<Map<String, Object>> ingestOtel(@RequestBody OtelExportDto dto) {
        UUID tenantId = TenantContext.get();

        Trace trace = otelMapper.fromOtel(dto);

        // Resolve agentId: if present in attributes and belongs to tenant, use it.
        // Otherwise require caller to pass agentId via attributes; if missing, reject.
        UUID agentId = trace.getAgentId();
        if (agentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Missing hookwatch.agent.id in resource attributes");
        }

        // Validate agent belongs to tenant
        if (tenantId != null) {
            agentRepository.findByIdAndTenantId(agentId, tenantId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "Agent does not belong to your tenant"));
        } else {
            agentRepository.findById(agentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Agent not found"));
        }

        // Wire up traceId on all spans
        Trace saved = persistTrace(trace, agentId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("traceId", saved.getId().toString());
        response.put("spanCount", saved.getSpans().size());
        response.put("status", saved.getStatus().name());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Persists a Trace (constructed from OTLP) with all its spans.
     * Save in two steps while keeping the same managed collection instance
     * to avoid orphan-removal dereference issues.
     */
    private Trace persistTrace(Trace trace, UUID agentId) {
        trace.setAgentId(agentId);

        // Ensure startedAt is set
        if (trace.getStartedAt() == null) {
            trace.setStartedAt(Instant.now());
        }

        if (trace.getStatus() != Trace.Status.RUNNING && trace.getCompletedAt() == null) {
            trace.setCompletedAt(Instant.now());
        }

        java.util.List<Span> incomingSpans = new java.util.ArrayList<>(trace.getSpans());
        trace.getSpans().clear();

        // 1) Persist trace first so it gets generated UUID
        Trace saved = traceRepository.save(trace);

        // 2) Attach spans to the same managed collection instance
        for (Span span : incomingSpans) {
            span.setId(null);
            span.setTraceId(saved.getId());
            if (span.getStartedAt() == null) span.setStartedAt(Instant.now());
            saved.getSpans().add(span);
        }

        return traceRepository.save(saved);
    }
}
