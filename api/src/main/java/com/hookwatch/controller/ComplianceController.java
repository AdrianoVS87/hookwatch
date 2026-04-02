package com.hookwatch.controller;

import com.hookwatch.dto.ApiErrorDto;
import com.hookwatch.dto.ComplianceReportDto;
import com.hookwatch.service.OtelComplianceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * OTel compliance validation endpoints for individual traces and agent-level summaries.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Compliance", description = "OpenTelemetry compliance validation and gap reporting")
public class ComplianceController {

    private final OtelComplianceService complianceService;

    @GetMapping("/api/v1/traces/{id}/compliance")
    @Operation(
            summary = "Validate trace OTel compliance",
            description = "Returns a compliance report with pass/fail per check and detailed gap list."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compliance report generated"),
            @ApiResponse(responseCode = "404", description = "Trace not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public ResponseEntity<ComplianceReportDto.TraceReport> validateTrace(@PathVariable UUID id) {
        return ResponseEntity.ok(complianceService.validateTrace(id));
    }

    @GetMapping("/api/v1/compliance/summary")
    @Operation(
            summary = "Get agent compliance summary",
            description = "Returns aggregate compliance score and top gaps across recent traces."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Summary loaded"),
            @ApiResponse(responseCode = "401", description = "Missing API key",
                    content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public ResponseEntity<ComplianceReportDto.AgentSummary> getAgentSummary(
            @RequestParam UUID agentId,
            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(complianceService.getAgentSummary(agentId, limit));
    }
}
