package com.hookwatch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Aggregated metrics for one agent")
public class AgentMetricsDto {
    @Schema(description = "Total number of traces", example = "120")
    private long totalTraces;

    @Schema(description = "Average token usage per trace", example = "842.7")
    private double avgTokens;

    @Schema(description = "Average cost per trace in USD", example = "0.018")
    private double avgCost;

    @Schema(description = "Trace success ratio in percentage", example = "97.5")
    private double successRate;

    @Schema(description = "95th percentile latency in milliseconds", example = "1375")
    private long p95LatencyMs;
}
