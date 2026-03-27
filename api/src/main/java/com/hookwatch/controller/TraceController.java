package com.hookwatch.controller;

import com.hookwatch.domain.Trace;
import com.hookwatch.dto.AnnotationCreateRequest;
import com.hookwatch.dto.AnnotationDto;
import com.hookwatch.dto.ApiErrorDto;
import com.hookwatch.dto.TraceComparisonDto;
import com.hookwatch.dto.TraceDto;
import com.hookwatch.dto.TraceTagsRequest;
import com.hookwatch.service.TraceComparisonService;
import com.hookwatch.service.TraceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/traces")
@RequiredArgsConstructor
@Tag(name = "Traces", description = "Trace ingestion, retrieval, and comparison")
public class TraceController {

    private final TraceService traceService;
    private final TraceComparisonService traceComparisonService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create trace with optional nested spans",
            description = "Stores a trace and optional list of spans in a single request payload."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Trace created"),
            @ApiResponse(responseCode = "401", description = "Missing API key", content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "403", description = "Invalid API key", content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "422", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public Trace create(@Valid @RequestBody TraceDto dto) {
        return traceService.create(dto);
    }

    @GetMapping
    @Operation(
            summary = "List traces by agent",
            description = "Returns paginated trace data for the given agentId. Optional ?tag filter uses PostgreSQL ANY over trace tags."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trace page loaded"),
            @ApiResponse(responseCode = "401", description = "Missing API key", content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "403", description = "Invalid API key", content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public Page<Trace> list(@RequestParam UUID agentId,
                            @RequestParam(required = false) String tag,
                            @PageableDefault(size = 20, sort = "startedAt") Pageable pageable) {
        return traceService.findByAgentId(agentId, pageable, tag);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get a trace with spans",
            description = "Returns trace detail and its related span collection."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trace found"),
            @ApiResponse(responseCode = "401", description = "Missing API key", content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "403", description = "Invalid API key", content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "404", description = "Trace not found", content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public ResponseEntity<Trace> get(@PathVariable UUID id) {
        return traceService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/tags")
    @Operation(
            summary = "Merge tags into a trace",
            description = "Deterministic merge: incoming + existing tags are normalized (trim + lowercase), deduplicated, and persisted sorted lexicographically."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trace tags updated"),
            @ApiResponse(responseCode = "401", description = "Missing API key", content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "403", description = "Trace belongs to another tenant", content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "404", description = "Trace not found", content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public Trace mergeTags(@PathVariable UUID id, @Valid @RequestBody TraceTagsRequest request) {
        return traceService.mergeTags(id, request.getTags());
    }

    @DeleteMapping("/{id}/tags/{tag}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete one tag from a trace")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Tag removed"),
            @ApiResponse(responseCode = "401", description = "Missing API key", content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "403", description = "Trace belongs to another tenant", content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "404", description = "Trace not found", content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public void deleteTag(@PathVariable UUID id, @PathVariable String tag) {
        traceService.deleteTag(id, tag);
    }

    @PostMapping("/{id}/annotations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create annotation for trace")
    public AnnotationDto createAnnotation(@PathVariable UUID id, @Valid @RequestBody AnnotationCreateRequest request) {
        return traceService.createAnnotation(id, request);
    }

    @GetMapping("/{id}/annotations")
    @Operation(summary = "List annotations for trace")
    public List<AnnotationDto> listAnnotations(@PathVariable UUID id) {
        return traceService.listAnnotations(id);
    }

    @GetMapping("/compare")
    @Operation(
            summary = "Compare two traces",
            description = "Compares two trace executions side-by-side and computes deltas for tokens, cost, latency, and status."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comparison generated"),
            @ApiResponse(responseCode = "401", description = "Missing API key", content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "403", description = "Invalid API key", content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "404", description = "Trace not found", content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public ResponseEntity<TraceComparisonDto> compare(@RequestParam UUID traceId1,
                                                       @RequestParam UUID traceId2) {
        return ResponseEntity.ok(traceComparisonService.compare(traceId1, traceId2));
    }
}
