package com.hookwatch.controller;

import com.hookwatch.dto.ScoreDto;
import com.hookwatch.dto.ScoreSummaryDto;
import com.hookwatch.service.ScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Scores")
public class ScoreController {

    private final ScoreService scoreService;

    @PostMapping("/api/v1/traces/{traceId}/scores")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a score to a trace")
    public ScoreDto create(@PathVariable UUID traceId, @Valid @RequestBody ScoreDto dto) {
        return scoreService.create(traceId, dto);
    }

    @GetMapping("/api/v1/traces/{traceId}/scores")
    @Operation(summary = "List all scores for a trace")
    public List<ScoreDto> listByTrace(@PathVariable UUID traceId) {
        return scoreService.listByTraceId(traceId);
    }

    @GetMapping("/api/v1/agents/{agentId}/scores/summary")
    @Operation(summary = "Get aggregated score summary for an agent")
    public ScoreSummaryDto summaryByAgent(@PathVariable UUID agentId) {
        return scoreService.summarizeByAgent(agentId);
    }
}
