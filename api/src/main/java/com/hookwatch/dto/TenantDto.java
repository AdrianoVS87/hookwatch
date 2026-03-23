package com.hookwatch.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TenantDto {
    @NotBlank
    private String name;
}
