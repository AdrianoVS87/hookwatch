package com.hookwatch.dto;

import com.hookwatch.domain.Trace;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class TraceDto {
    @NotNull
    private UUID agentId;

    @NotNull
    private Trace.Status status;

    private Integer totalTokens;
    private BigDecimal totalCost;
    private Map<String, Object> metadata;
    private List<SpanDto> spans;
}
