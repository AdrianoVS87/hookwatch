package com.hookwatch.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApiError", description = "Error payload returned by API and auth filter")
public record ApiErrorDto(
        @Schema(description = "Human-readable error message", example = "Invalid API key")
        String error
) {}
