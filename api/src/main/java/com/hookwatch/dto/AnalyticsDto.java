package com.hookwatch.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Analytics response for an agent in a date range")
public record AnalyticsDto(
        @Schema(description = "Daily usage buckets")
        List<DailyUsage> dailyUsage,
        @Schema(description = "Usage grouped by model")
        List<ModelUsage> byModel,
        @Schema(description = "Most expensive traces")
        List<TopTrace> topExpensiveTraces,
        @Schema(description = "Cost trend and projection")
        CostTrend costTrend
) {

    @Schema(description = "Daily usage aggregate")
    public record DailyUsage(
            @Schema(description = "Date bucket (ISO-8601)", example = "2026-03-26")
            String date,
            @Schema(description = "Total tokens", example = "13820")
            int totalTokens,
            @Schema(description = "Total cost in USD", example = "2.41")
            double totalCost,
            @Schema(description = "Trace count", example = "42")
            int traceCount,
            @Schema(description = "Average latency in milliseconds", example = "702.3")
            double avgLatencyMs,
            @Schema(description = "Error rate percentage", example = "4.8")
            double errorRate
    ) {}

    @Schema(description = "Model usage aggregate")
    public record ModelUsage(
            @Schema(description = "Model identifier", example = "claude-sonnet-4-6")
            String model,
            @Schema(description = "Total tokens", example = "8200")
            int totalTokens,
            @Schema(description = "Total cost in USD", example = "1.27")
            double totalCost,
            @Schema(description = "Trace count", example = "18")
            int traceCount
    ) {}

    @Schema(description = "Top expensive trace entry")
    public record TopTrace(
            @Schema(description = "Trace id", example = "d3f5c7b2-f47b-4d5f-b765-28ef4fc819f8")
            String traceId,
            @Schema(description = "Total cost in USD", example = "0.39")
            double totalCost,
            @Schema(description = "Total tokens", example = "3200")
            int totalTokens,
            @Schema(description = "Trace start timestamp", example = "2026-03-26T21:33:40Z")
            String startedAt
    ) {}

    @Schema(description = "Cost trend summary")
    public record CostTrend(
            @Schema(description = "Cost percentage change against previous period", example = "13.2")
            double percentChangeVsPreviousPeriod,
            @Schema(description = "Projected monthly cost in USD", example = "54.7")
            double projectedMonthlyCost
    ) {}
}
