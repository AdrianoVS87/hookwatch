package com.hookwatch.controller;

import com.hookwatch.dto.ApiErrorDto;
import com.hookwatch.dto.FingerprintDto;
import com.hookwatch.service.FingerprintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Exposes failure fingerprint grouping and daily trend data.
 */
@RestController
@RequestMapping("/api/v1/fingerprints")
@RequiredArgsConstructor
@Tag(name = "Fingerprints", description = "Failure fingerprint tracking and trend analysis")
public class FingerprintController {

    private final FingerprintService fingerprintService;

    @GetMapping
    @Operation(
            summary = "List failure fingerprints for an agent",
            description = "Returns all fingerprints grouped by error hash, sorted by occurrence count descending."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fingerprints loaded"),
            @ApiResponse(responseCode = "401", description = "Missing API key",
                    content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public ResponseEntity<List<FingerprintDto.FingerprintSummary>> getFingerprints(
            @RequestParam UUID agentId) {
        return ResponseEntity.ok(fingerprintService.getFingerprints(agentId));
    }

    @GetMapping("/{id}/trend")
    @Operation(
            summary = "Get daily trend for a fingerprint",
            description = "Returns daily occurrence counts for the specified fingerprint within the given date range."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trend loaded"),
            @ApiResponse(responseCode = "404", description = "Fingerprint not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public ResponseEntity<FingerprintDto.TrendResponse> getTrend(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(fingerprintService.getTrend(id, from, to));
    }
}
