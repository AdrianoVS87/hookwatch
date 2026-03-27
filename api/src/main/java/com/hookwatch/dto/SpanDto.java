package com.hookwatch.dto;

import com.hookwatch.domain.Span;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Schema(description = "Span payload nested inside trace ingestion")
public class SpanDto {
    @Schema(description = "Parent span id for hierarchy", example = "2d03adfb-1134-4b16-a7cd-a29d6fd1752c")
    private UUID parentSpanId;

    @NotBlank
    @Schema(description = "Span name", example = "claude-completion")
    private String name;

    @NotNull
    @Schema(description = "Span type", example = "LLM_CALL")
    private Span.Type type;

    @NotNull
    @Schema(description = "Span execution status", example = "COMPLETED")
    private Span.Status status;

    @Schema(description = "Input token count", example = "420")
    private Integer inputTokens;

    @Schema(description = "Output token count", example = "780")
    private Integer outputTokens;

    @Schema(description = "Cost in USD", example = "0.0125")
    private BigDecimal cost;

    @Schema(description = "LLM model identifier", example = "claude-sonnet-4-6")
    private String model;

    @Schema(description = "Input payload captured for debugging", example = "Summarize this diff")
    private String input;

    @Schema(description = "Output payload captured for debugging", example = "Here is the summary...")
    private String output;

    @Schema(description = "Error details when status is FAILED", example = "Rate limit exceeded")
    private String error;

    @Schema(description = "Ordering index within trace", example = "1")
    private Integer sortOrder;
}
