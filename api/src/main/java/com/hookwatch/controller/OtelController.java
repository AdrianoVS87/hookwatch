package com.hookwatch.controller;

import com.hookwatch.domain.Agent;
import com.hookwatch.domain.Span;
import com.hookwatch.domain.Trace;
import com.hookwatch.dto.ApiErrorDto;
import com.hookwatch.dto.OtelExportDto;
import com.hookwatch.repository.AgentRepository;
import com.hookwatch.repository.TraceRepository;
import com.hookwatch.security.TenantContext;
import com.hookwatch.service.OtelMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@Tag(name = "Traces", description = "OTEL import/export interoperability for traces")
public class OtelController {

    private final TraceRepository traceRepository;
    private final AgentRepository agentRepository;
    private final OtelMapper otelMapper;

    @GetMapping("/api/v1/traces/{id}/otel")
    @Operation(
            summary = "Export trace as OTLP JSON",
            description = "Converts a stored HookWatch trace into OTLP JSON-compatible structure."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OTLP export generated"),
            @ApiResponse(responseCode = "401", description = "Missing API key", content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "403", description = "Trace belongs to another tenant", content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "404", description = "Trace or agent not found", content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public ResponseEntity<OtelExportDto> exportOtel(@PathVariable UUID id) {
        UUID tenantId = TenantContext.get();

        Trace trace;
        if (tenantId != null) {
            trace = traceRepository.findByIdAndTenantId(id, tenantId)
                    .orElseThrow(() -> {
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

    @PostMapping("/api/v1/ingest/otel")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Ingest OTLP JSON trace",
            description = "Parses OTLP JSON payload and persists it as HookWatch trace + spans."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Trace ingested"),
            @ApiResponse(responseCode = "401", description = "Missing API key", content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "403", description = "Agent belongs to another tenant", content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "404", description = "Agent not found", content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "422", description = "Invalid OTLP payload", content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public ResponseEntity<Map<String, Object>> ingestOtel(@RequestBody OtelExportDto dto) {
        UUID tenantId = TenantContext.get();

        Trace trace = otelMapper.fromOtel(dto);

        UUID agentId = trace.getAgentId();
        if (agentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Missing hookwatch.agent.id in resource attributes");
        }

        if (tenantId != null) {
            agentRepository.findByIdAndTenantId(agentId, tenantId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "Agent does not belong to your tenant"));
        } else {
            agentRepository.findById(agentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Agent not found"));
        }

        Trace saved = persistTrace(trace, agentId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("traceId", saved.getId().toString());
        response.put("spanCount", saved.getSpans().size());
        response.put("status", saved.getStatus().name());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private Trace persistTrace(Trace trace, UUID agentId) {
        trace.setAgentId(agentId);

        if (trace.getStartedAt() == null) {
            trace.setStartedAt(Instant.now());
        }

        if (trace.getStatus() != Trace.Status.RUNNING && trace.getCompletedAt() == null) {
            trace.setCompletedAt(Instant.now());
        }

        java.util.List<Span> incomingSpans = new java.util.ArrayList<>(trace.getSpans());
        trace.getSpans().clear();

        Trace saved = traceRepository.save(trace);

        for (Span span : incomingSpans) {
            span.setId(null);
            span.setTraceId(saved.getId());
            if (span.getStartedAt() == null) span.setStartedAt(Instant.now());
            saved.getSpans().add(span);
        }

        return traceRepository.save(saved);
    }
}
