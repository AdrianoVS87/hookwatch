package com.hookwatch.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AgentMetricsDto {
    private long totalTraces;
    private double avgTokens;
    private double avgCost;
    private double successRate;
    private long p95LatencyMs;
}
