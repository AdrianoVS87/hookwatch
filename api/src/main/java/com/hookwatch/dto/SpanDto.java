package com.hookwatch.dto;

import com.hookwatch.domain.Span;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class SpanDto {
    private UUID parentSpanId;

    @NotBlank
    private String name;

    @NotNull
    private Span.Type type;

    @NotNull
    private Span.Status status;

    private Integer inputTokens;
    private Integer outputTokens;
    private BigDecimal cost;
    private String model;
    private String input;
    private String output;
    private String error;
    private Integer sortOrder;
}
