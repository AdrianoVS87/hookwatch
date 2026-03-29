package com.hookwatch.dto;

import com.hookwatch.domain.Trace;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Side-by-side trace comparison result")
public record TraceComparisonDto(
        @Schema(description = "Left trace")
        Trace trace1,
        @Schema(description = "Right trace")
        Trace trace2,
        @Schema(description = "Computed deltas")
        Delta delta
) {
    public record Delta(
            @Schema(description = "Token count difference", example = "-120")
            int tokensDiff,
            @Schema(description = "Cost difference in USD", example = "-0.004")
            double costDiff,
            @Schema(description = "Latency difference in milliseconds", example = "85")
            long latencyDiffMs,
            @Schema(description = "Whether statuses are equal", example = "true")
            boolean statusMatch,
            @Schema(description = "Span count difference", example = "2")
            int spanCountDiff,
            @Schema(description = "Span-by-span comparisons")
            List<SpanComparison> spanBySpanComparison
    ) {}

    public record SpanComparison(
            @Schema(description = "Name of span from first trace", example = "retrieval")
            String span1Name,
            @Schema(description = "Name of span from second trace", example = "retrieval")
            String span2Name,
            @Schema(description = "Token difference", example = "40")
            int tokensDiff,
            @Schema(description = "Latency difference in milliseconds", example = "12")
            long latencyDiffMs,
            @Schema(description = "Whether statuses are equal", example = "true")
            boolean statusMatch
    ) {}
}
