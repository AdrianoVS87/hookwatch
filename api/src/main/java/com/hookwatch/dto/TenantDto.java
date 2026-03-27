package com.hookwatch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Tenant creation payload")
public class TenantDto {
    @NotBlank
    @Schema(description = "Unique tenant name", example = "acme-ai")
    private String name;
}
