package com.hookwatch.controller;

import com.hookwatch.domain.Trace;
import com.hookwatch.dto.TraceComparisonDto;
import com.hookwatch.dto.TraceDto;
import com.hookwatch.service.TraceComparisonService;
import com.hookwatch.service.TraceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/traces")
@RequiredArgsConstructor
@Tag(name = "Traces")
public class TraceController {

    private final TraceService traceService;
    private final TraceComparisonService traceComparisonService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a trace with optional nested spans")
    public Trace create(@Valid @RequestBody TraceDto dto) {
        return traceService.create(dto);
    }

    @GetMapping
    @Operation(summary = "List traces by agent (paginated)")
    public Page<Trace> list(@RequestParam UUID agentId,
                            @PageableDefault(size = 20, sort = "startedAt") Pageable pageable) {
        return traceService.findByAgentId(agentId, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a trace with its spans")
    public ResponseEntity<Trace> get(@PathVariable UUID id) {
        return traceService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/compare")
    @Operation(summary = "Compare two traces side-by-side with computed deltas")
    public ResponseEntity<TraceComparisonDto> compare(@RequestParam UUID traceId1,
                                                       @RequestParam UUID traceId2) {
        return ResponseEntity.ok(traceComparisonService.compare(traceId1, traceId2));
    }
}
