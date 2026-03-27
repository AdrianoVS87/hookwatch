package com.hookwatch.controller;

import com.hookwatch.dto.AnalyticsDto;
import com.hookwatch.dto.ApiErrorDto;
import com.hookwatch.repository.AgentRepository;
import com.hookwatch.security.TenantContext;
import com.hookwatch.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Aggregated usage, cost, and latency analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final AgentRepository agentRepository;

    @GetMapping
    @Operation(
            summary = "Get analytics for an agent",
            description = "Returns daily usage aggregation, model usage, expensive traces, and cost trend for the given date window."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Analytics loaded"),
            @ApiResponse(responseCode = "401", description = "Missing API key", content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "403", description = "Agent does not belong to tenant", content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "404", description = "Agent not found", content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "422", description = "Invalid query input", content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public ResponseEntity<AnalyticsDto> getAnalytics(
            @RequestParam UUID agentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "day") String granularity) {

        UUID tenantId = TenantContext.get();

        if (tenantId != null) {
            agentRepository.findByIdAndTenantId(agentId, tenantId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "Agent does not belong to your tenant"));
        } else {
            agentRepository.findById(agentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Agent not found"));
        }

        if (from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "'from' date must be before or equal to 'to' date");
        }

        AnalyticsDto result = analyticsService.getAnalytics(agentId, from, to, granularity);
        return ResponseEntity.ok(result);
    }
}
