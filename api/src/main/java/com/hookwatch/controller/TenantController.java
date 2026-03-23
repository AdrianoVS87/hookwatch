package com.hookwatch.controller;

import com.hookwatch.domain.Tenant;
import com.hookwatch.dto.TenantDto;
import com.hookwatch.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenants")
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a tenant and receive an API key")
    public Tenant create(@Valid @RequestBody TenantDto dto) {
        return tenantService.create(dto);
    }
}
