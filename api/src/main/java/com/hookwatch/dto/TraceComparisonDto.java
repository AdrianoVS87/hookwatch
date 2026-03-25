package com.hookwatch.dto;

import java.util.List;

/**
 * Side-by-side comparison of two traces with computed deltas.
 * Inspired by LangSmith comparison view for prompt/model evaluation.
 */
public record TraceComparisonDto(
    TraceSummary trace1,
    TraceSummary trace2,
    Delta delta
) {
    public record TraceSummary(
        String id,
        String status,
        Integer totalTokens,
        Double totalCost,
        int spanCount,
        String startedAt,
        String completedAt
    ) {}

    public record Delta(
        int tokensDiff,
        double costDiff,
        long latencyDiffMs,
        boolean statusMatch,
        int spanCountDiff,
        List<SpanComparison> spanComparisons
    ) {}

    public record SpanComparison(
        String span1Name,
        String span2Name,
        Integer tokensDiff,
        Long latencyDiffMs,
        boolean statusMatch
    ) {}
}
