package com.hookwatch.dto;

import java.util.List;

public record AnalyticsDto(
        List<DailyUsage> dailyUsage,
        List<ModelUsage> byModel,
        List<TopTrace> topExpensiveTraces,
        CostTrend costTrend
) {

    public record DailyUsage(
            String date,
            int totalTokens,
            double totalCost,
            int traceCount,
            double avgLatencyMs,
            double errorRate
    ) {}

    public record ModelUsage(
            String model,
            int totalTokens,
            double totalCost,
            int traceCount
    ) {}

    public record TopTrace(
            String traceId,
            double totalCost,
            int totalTokens,
            String startedAt
    ) {}

    public record CostTrend(
            double percentChangeVsPreviousPeriod,
            double projectedMonthlyCost
    ) {}
}
