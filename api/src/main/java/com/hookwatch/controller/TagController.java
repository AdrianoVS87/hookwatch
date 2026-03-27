package com.hookwatch.controller;

import com.hookwatch.dto.ApiErrorDto;
import com.hookwatch.service.TraceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
@Tag(name = "Tags", description = "Tenant-scoped trace tags")
public class TagController {

    private final TraceService traceService;

    @GetMapping
    @Operation(summary = "List unique trace tags for current tenant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tags loaded"),
            @ApiResponse(responseCode = "401", description = "Missing API key", content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "403", description = "Invalid API key", content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public List<String> listTags() {
        return traceService.listUniqueTags();
    }
}
