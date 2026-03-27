package com.hookwatch.dto;

import com.hookwatch.domain.Trace;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Schema(description = "Trace ingestion payload")
public class TraceDto {
    @NotNull
    @Schema(description = "Agent id that produced this trace", example = "e8b0ca7a-a4f8-4118-a147-f4d859af5929")
    private UUID agentId;

    @NotNull
    @Schema(description = "Trace status", example = "COMPLETED")
    private Trace.Status status;

    @Schema(description = "Total token count for the trace", example = "1200")
    private Integer totalTokens;

    @Schema(description = "Total trace cost in USD", example = "0.024")
    private BigDecimal totalCost;

    @Schema(description = "Free-form trace metadata")
    private Map<String, Object> metadata;

    @Schema(description = "Nested span list")
    private List<SpanDto> spans;
}
