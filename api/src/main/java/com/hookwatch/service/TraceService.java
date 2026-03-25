package com.hookwatch.service;

import com.hookwatch.domain.Span;
import com.hookwatch.domain.Trace;
import com.hookwatch.dto.SpanDto;
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

import java.time.Instant;
import java.util.ArrayList;
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
