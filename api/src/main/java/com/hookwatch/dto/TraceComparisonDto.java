package com.hookwatch.dto;

import com.hookwatch.domain.Trace;
import java.util.List;

/**
 * Side-by-side trace comparison result.
 * Inspired by LangSmith comparison view for evaluating prompt/model changes.
 */
public record TraceComparisonDto(
    Trace trace1,
    Trace trace2,
    Delta delta
) {
    public record Delta(
        int tokensDiff,
        double costDiff,
        long latencyDiffMs,
        boolean statusMatch,
        int spanCountDiff,
        List<SpanComparison> spanBySpanComparison
    ) {}

    public record SpanComparison(
        String span1Name,
        String span2Name,
        int tokensDiff,
        long latencyDiffMs,
        boolean statusMatch
    ) {}
}
