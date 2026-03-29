package com.hookwatch.controller;

import com.hookwatch.domain.Tenant;
import com.hookwatch.dto.ApiErrorDto;
import com.hookwatch.dto.TenantDto;
import com.hookwatch.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenants", description = "Tenant bootstrap and provisioning")
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a tenant and issue API key",
            description = "Bootstraps a new tenant account and returns tenant metadata including API key hash."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tenant created"),
            @ApiResponse(responseCode = "422", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public Tenant create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Tenant creation payload",
                    required = true,
                    content = @Content(examples = @ExampleObject(value = "{\"name\":\"acme-ai\"}"))
            )
            @Valid @RequestBody TenantDto dto) {
        return tenantService.create(dto);
    }
}
