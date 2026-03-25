package com.hookwatch.service;

import com.hookwatch.domain.Span;
import com.hookwatch.domain.Trace;
import com.hookwatch.dto.SpanDto;
import com.hookwatch.dto.TraceComparisonDto;
import com.hookwatch.dto.TraceDto;
import com.hookwatch.repository.TraceRepository;
import com.hookwatch.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TraceService {

    private final TraceRepository traceRepository;
    private final TraceEventPublisher eventPublisher;

    @Transactional
    public Trace create(TraceDto dto) {
        Trace trace = Trace.builder()
                .agentId(dto.getAgentId())
                .status(dto.getStatus())
                .totalTokens(dto.getTotalTokens())
                .totalCost(dto.getTotalCost())
                .metadata(dto.getMetadata())
                .build();

        if (dto.getStatus() != Trace.Status.RUNNING) {
            trace.setCompletedAt(Instant.now());
        }

        // Use mutable ArrayList — Hibernate requires clear() on the collection during merge
        List<Span> spans = new ArrayList<>();
        if (dto.getSpans() != null) {
            for (SpanDto spanDto : dto.getSpans()) {
                Span span = mapSpan(spanDto);
                span.setTraceId(trace.getId());
                spans.add(span);
            }
        }

        Trace saved = traceRepository.save(trace);

        spans.forEach(s -> s.setTraceId(saved.getId()));
        saved.setSpans(spans);
        Trace result = traceRepository.save(saved);

        eventPublisher.publish(result.getId(), result);
        return result;
    }

    /**
     * Lists traces for an agent, scoped to the authenticated tenant.
     * Prevents cross-tenant access: if agentId does not belong to the tenant,
     * an empty page is returned (no 404 leak).
     */
    @Transactional(readOnly = true)
    public Page<Trace> findByAgentId(UUID agentId, Pageable pageable) {
        UUID tenantId = TenantContext.get();
        if (tenantId != null) {
            return traceRepository.findByAgentIdAndTenantId(agentId, tenantId, pageable);
        }
        return traceRepository.findByAgentId(agentId, pageable);
    }

    /**
     * Finds a trace by ID, verifying it belongs to the authenticated tenant.
     * Throws 403 if the trace exists but belongs to a different tenant.
     */
    @Transactional(readOnly = true)
    public Optional<Trace> findById(UUID id) {
        UUID tenantId = TenantContext.get();
        if (tenantId != null) {
            Optional<Trace> trace = traceRepository.findByIdAndTenantId(id, tenantId);
            if (trace.isEmpty() && traceRepository.existsById(id)) {
                // Trace exists but doesn't belong to this tenant — 403
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Access denied: trace does not belong to your tenant");
            }
            return trace;
        }
        return traceRepository.findById(id);
    }

    /**
     * Compares two traces side-by-side with computed deltas.
     * Spans are matched by sortOrder for comparison.
     */
    @Transactional(readOnly = true)
    public TraceComparisonDto compare(UUID traceId1, UUID traceId2) {
        Trace t1 = findByIdOrThrow(traceId1, "Trace 1 not found");
        Trace t2 = findByIdOrThrow(traceId2, "Trace 2 not found");

        int tok1 = t1.getTotalTokens() != null ? t1.getTotalTokens() : 0;
        int tok2 = t2.getTotalTokens() != null ? t2.getTotalTokens() : 0;
        double cost1 = t1.getTotalCost() != null ? t1.getTotalCost().doubleValue() : 0;
        double cost2 = t2.getTotalCost() != null ? t2.getTotalCost().doubleValue() : 0;

        long latency1 = t1.getCompletedAt() != null && t1.getStartedAt() != null
                ? Duration.between(t1.getStartedAt(), t1.getCompletedAt()).toMillis() : 0;
        long latency2 = t2.getCompletedAt() != null && t2.getStartedAt() != null
                ? Duration.between(t2.getStartedAt(), t2.getCompletedAt()).toMillis() : 0;

        boolean statusMatch = t1.getStatus() == t2.getStatus();
        int spanCountDiff = t2.getSpans().size() - t1.getSpans().size();

        List<Span> spans1 = t1.getSpans().stream()
                .sorted(Comparator.comparingInt(s -> s.getSortOrder() != null ? s.getSortOrder() : 0)).toList();
        List<Span> spans2 = t2.getSpans().stream()
                .sorted(Comparator.comparingInt(s -> s.getSortOrder() != null ? s.getSortOrder() : 0)).toList();

        List<TraceComparisonDto.SpanComparison> spanComps = new ArrayList<>();
        int maxSpans = Math.max(spans1.size(), spans2.size());
        for (int i = 0; i < maxSpans; i++) {
            Span s1 = i < spans1.size() ? spans1.get(i) : null;
            Span s2 = i < spans2.size() ? spans2.get(i) : null;

            Integer sTokDiff = null;
            Long sLatDiff = null;
            boolean sStatusMatch = false;

            if (s1 != null && s2 != null) {
                int st1 = (s1.getInputTokens() != null ? s1.getInputTokens() : 0)
                        + (s1.getOutputTokens() != null ? s1.getOutputTokens() : 0);
                int st2 = (s2.getInputTokens() != null ? s2.getInputTokens() : 0)
                        + (s2.getOutputTokens() != null ? s2.getOutputTokens() : 0);
                sTokDiff = st2 - st1;
                long sl1 = s1.getCompletedAt() != null && s1.getStartedAt() != null
                        ? Duration.between(s1.getStartedAt(), s1.getCompletedAt()).toMillis() : 0;
                long sl2 = s2.getCompletedAt() != null && s2.getStartedAt() != null
                        ? Duration.between(s2.getStartedAt(), s2.getCompletedAt()).toMillis() : 0;
                sLatDiff = sl2 - sl1;
                sStatusMatch = s1.getStatus() == s2.getStatus();
            }

            spanComps.add(new TraceComparisonDto.SpanComparison(
                    s1 != null ? s1.getName() : "(missing)",
                    s2 != null ? s2.getName() : "(missing)",
                    sTokDiff, sLatDiff, sStatusMatch
            ));
        }

        return new TraceComparisonDto(
                toSummary(t1), toSummary(t2),
                new TraceComparisonDto.Delta(tok2 - tok1, cost2 - cost1, latency2 - latency1,
                        statusMatch, spanCountDiff, spanComps)
        );
    }

    private Trace findByIdOrThrow(UUID id, String message) {
        UUID tenantId = TenantContext.get();
        if (tenantId != null) {
            Optional<Trace> trace = traceRepository.findByIdAndTenantId(id, tenantId);
            if (trace.isEmpty()) {
                if (traceRepository.existsById(id)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
                }
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
            }
            return trace.get();
        }
        return traceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, message));
    }

    private TraceComparisonDto.TraceSummary toSummary(Trace t) {
        return new TraceComparisonDto.TraceSummary(
                t.getId().toString(),
                t.getStatus().name(),
                t.getTotalTokens(),
                t.getTotalCost() != null ? t.getTotalCost().doubleValue() : null,
                t.getSpans().size(),
                t.getStartedAt() != null ? t.getStartedAt().toString() : null,
                t.getCompletedAt() != null ? t.getCompletedAt().toString() : null
        );
    }

    private Span mapSpan(SpanDto dto) {
        return Span.builder()
                .parentSpanId(dto.getParentSpanId())
                .name(dto.getName())
                .type(dto.getType())
                .status(dto.getStatus())
                .inputTokens(dto.getInputTokens())
                .outputTokens(dto.getOutputTokens())
                .cost(dto.getCost())
                .model(dto.getModel())
                .input(dto.getInput())
                .output(dto.getOutput())
                .error(dto.getError())
                .sortOrder(dto.getSortOrder())
                .build();
    }
}
