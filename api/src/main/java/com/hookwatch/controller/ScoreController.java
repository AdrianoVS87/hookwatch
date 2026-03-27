package com.hookwatch.controller;

import com.hookwatch.dto.ApiErrorDto;
import com.hookwatch.dto.ScoreDto;
import com.hookwatch.dto.ScoreSummaryDto;
import com.hookwatch.service.ScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Scores", description = "Scoring APIs for traces and agents")
public class ScoreController {

    private final ScoreService scoreService;

    @PostMapping("/api/v1/traces/{traceId}/scores")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Add score to trace",
            description = "Creates a typed score record linked to a given trace."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Score created"),
            @ApiResponse(responseCode = "401", description = "Missing API key", content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "403", description = "Invalid API key", content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "404", description = "Trace not found", content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "422", description = "Invalid score payload", content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public ScoreDto create(@PathVariable UUID traceId, @Valid @RequestBody ScoreDto dto) {
        return scoreService.create(traceId, dto);
    }

    @GetMapping("/api/v1/traces/{traceId}/scores")
    @Operation(
            summary = "List scores for trace",
            description = "Returns all score records associated with a trace."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Scores listed"),
            @ApiResponse(responseCode = "401", description = "Missing API key", content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "403", description = "Invalid API key", content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "404", description = "Trace not found", content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public List<ScoreDto> listByTrace(@PathVariable UUID traceId) {
        return scoreService.listByTraceId(traceId);
    }

    @GetMapping("/api/v1/agents/{agentId}/scores/summary")
    @Operation(
            summary = "Get score summary by agent",
            description = "Returns aggregated score statistics grouped by numeric and categorical metrics."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Score summary loaded"),
            @ApiResponse(responseCode = "401", description = "Missing API key", content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "403", description = "Invalid API key", content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "404", description = "Agent not found", content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public ScoreSummaryDto summaryByAgent(@PathVariable UUID agentId) {
        return scoreService.summarizeByAgent(agentId);
    }
}
