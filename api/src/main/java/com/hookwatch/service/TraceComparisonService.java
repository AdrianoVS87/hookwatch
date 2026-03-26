package com.hookwatch.service;

import com.hookwatch.domain.Span;
import com.hookwatch.domain.Trace;
import com.hookwatch.dto.TraceComparisonDto;
import com.hookwatch.repository.TraceRepository;
import com.hookwatch.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Compares two traces side-by-side with computed deltas.
 */
@Service
@RequiredArgsConstructor
public class TraceComparisonService {

    private final TraceRepository traceRepository;

    /**
     * Compares two traces and returns a side-by-side result with deltas.
     *
     * @throws ResponseStatusException 404 if either trace not found, 403 if wrong tenant
     */
    @Transactional(readOnly = true)
    public TraceComparisonDto compare(UUID traceId1, UUID traceId2) {
        Trace trace1 = loadTrace(traceId1);
        Trace trace2 = loadTrace(traceId2);

        int tokens1 = trace1.getTotalTokens() != null ? trace1.getTotalTokens() : 0;
        int tokens2 = trace2.getTotalTokens() != null ? trace2.getTotalTokens() : 0;
        double cost1 = trace1.getTotalCost() != null ? trace1.getTotalCost().doubleValue() : 0.0;
        double cost2 = trace2.getTotalCost() != null ? trace2.getTotalCost().doubleValue() : 0.0;
        long latency1 = latencyMs(trace1);
        long latency2 = latencyMs(trace2);

        List<TraceComparisonDto.SpanComparison> spanComparisons = buildSpanComparisons(
                trace1.getSpans(), trace2.getSpans());

        TraceComparisonDto.Delta delta = new TraceComparisonDto.Delta(
                tokens2 - tokens1,
                cost2 - cost1,
                latency2 - latency1,
                trace1.getStatus() == trace2.getStatus(),
                trace2.getSpans().size() - trace1.getSpans().size(),
                spanComparisons
        );

        return new TraceComparisonDto(trace1, trace2, delta);
    }

    private Trace loadTrace(UUID id) {
        UUID tenantId = TenantContext.get();
        if (tenantId != null) {
            var trace = traceRepository.findByIdAndTenantId(id, tenantId);
            if (trace.isEmpty() && traceRepository.existsById(id)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Access denied: trace does not belong to your tenant");
            }
            return trace.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Trace not found: " + id));
        }
        return traceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Trace not found: " + id));
    }

    private long latencyMs(Trace trace) {
        if (trace.getCompletedAt() == null || trace.getStartedAt() == null) {
            return 0;
        }
        return Duration.between(trace.getStartedAt(), trace.getCompletedAt()).toMillis();
    }

    private List<TraceComparisonDto.SpanComparison> buildSpanComparisons(
            List<Span> spans1, List<Span> spans2) {
        List<Span> sorted1 = spans1.stream()
                .sorted(Comparator.comparingInt(s -> s.getSortOrder() != null ? s.getSortOrder() : 0))
                .toList();
        List<Span> sorted2 = spans2.stream()
                .sorted(Comparator.comparingInt(s -> s.getSortOrder() != null ? s.getSortOrder() : 0))
                .toList();

        int maxLen = Math.max(sorted1.size(), sorted2.size());
        List<TraceComparisonDto.SpanComparison> comparisons = new ArrayList<>();

        for (int i = 0; i < maxLen; i++) {
            Span s1 = i < sorted1.size() ? sorted1.get(i) : null;
            Span s2 = i < sorted2.size() ? sorted2.get(i) : null;

            int t1 = spanTokens(s1);
            int t2 = spanTokens(s2);
            long l1 = spanLatencyMs(s1);
            long l2 = spanLatencyMs(s2);

            comparisons.add(new TraceComparisonDto.SpanComparison(
                    s1 != null ? s1.getName() : null,
                    s2 != null ? s2.getName() : null,
                    t2 - t1,
                    l2 - l1,
                    s1 != null && s2 != null && s1.getStatus() == s2.getStatus()
            ));
        }
        return comparisons;
    }

    private int spanTokens(Span span) {
        if (span == null) return 0;
        int input = span.getInputTokens() != null ? span.getInputTokens() : 0;
        int output = span.getOutputTokens() != null ? span.getOutputTokens() : 0;
        return input + output;
    }

    private long spanLatencyMs(Span span) {
        if (span == null || span.getCompletedAt() == null || span.getStartedAt() == null) return 0;
        return Duration.between(span.getStartedAt(), span.getCompletedAt()).toMillis();
    }
}
