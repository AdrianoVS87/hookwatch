package com.hookwatch.service;

import com.hookwatch.domain.Annotation;
import com.hookwatch.domain.Span;
import com.hookwatch.domain.Trace;
import com.hookwatch.dto.AnnotationCreateRequest;
import com.hookwatch.dto.AnnotationDto;
import com.hookwatch.dto.SpanDto;
import com.hookwatch.dto.TraceDto;
import com.hookwatch.repository.AnnotationRepository;
import com.hookwatch.repository.TraceRepository;
import com.hookwatch.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TraceService {

    private final TraceRepository traceRepository;
    private final AnnotationRepository annotationRepository;
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
    public Page<Trace> findByAgentId(UUID agentId, Pageable pageable, String tag) {
        UUID tenantId = TenantContext.get();
        String normalizedTag = normalizeTag(tag);
        if (tenantId != null) {
            if (normalizedTag != null) {
                Pageable tagPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
                return traceRepository.findByAgentIdAndTenantIdAndTag(agentId, tenantId, normalizedTag, tagPageable);
            }
            return traceRepository.findByAgentIdAndTenantId(agentId, tenantId, pageable);
        }
        if (normalizedTag != null) {
            Pageable tagPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
            return traceRepository.findByAgentIdAndTag(agentId, normalizedTag, tagPageable);
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
     * Deterministic merge behavior: tags are normalized (trim + lowercase), deduplicated,
     * unioned with existing tags, and persisted sorted in lexicographic order.
     */
    @Transactional
    public Trace mergeTags(UUID traceId, List<String> requestedTags) {
        Trace trace = resolveTraceForTenant(traceId, TenantContext.get());
        SortedSet<String> merged = new TreeSet<>();
        merged.addAll(normalizeTags(Arrays.asList(trace.getTags())));
        merged.addAll(normalizeTags(requestedTags));
        trace.setTags(merged.toArray(String[]::new));
        return traceRepository.save(trace);
    }

    @Transactional
    public Trace deleteTag(UUID traceId, String tag) {
        Trace trace = resolveTraceForTenant(traceId, TenantContext.get());
        String normalized = normalizeTag(tag);
        if (normalized == null) {
            return trace;
        }

        List<String> kept = normalizeTags(Arrays.asList(trace.getTags())).stream()
                .filter(existing -> !existing.equals(normalized))
                .toList();

        trace.setTags(kept.toArray(String[]::new));
        return traceRepository.save(trace);
    }

    @Transactional(readOnly = true)
    public List<String> listUniqueTags() {
        UUID tenantId = TenantContext.get();
        if (tenantId != null) {
            return traceRepository.findUniqueTagsByTenantId(tenantId);
        }
        return traceRepository.findUniqueTags();
    }

    @Transactional
    public AnnotationDto createAnnotation(UUID traceId, AnnotationCreateRequest dto) {
        Trace trace = resolveTraceForTenant(traceId, TenantContext.get());
        Annotation saved = annotationRepository.save(Annotation.builder()
                .trace(trace)
                .text(dto.getText().trim())
                .author(dto.getAuthor().trim())
                .build());
        return AnnotationDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<AnnotationDto> listAnnotations(UUID traceId) {
        resolveTraceForTenant(traceId, TenantContext.get());
        return annotationRepository.findByTrace_IdOrderByCreatedAtDesc(traceId)
                .stream()
                .map(AnnotationDto::fromEntity)
                .toList();
    }

    private Trace resolveTraceForTenant(UUID traceId, UUID tenantId) {
        if (tenantId != null) {
            Optional<Trace> trace = traceRepository.findByIdAndTenantId(traceId, tenantId);
            if (trace.isEmpty()) {
                if (traceRepository.existsById(traceId)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "Access denied: trace does not belong to your tenant");
                }
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Trace not found");
            }
            return trace.get();
        }
        return traceRepository.findById(traceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trace not found"));
    }

    private List<String> normalizeTags(Collection<String> tags) {
        if (tags == null) {
            return List.of();
        }
        return tags.stream()
                .map(this::normalizeTag)
                .filter(Objects::nonNull)
                .toList();
    }

    private String normalizeTag(String rawTag) {
        if (rawTag == null) {
            return null;
        }
        String value = rawTag.trim().toLowerCase(Locale.ROOT);
        return value.isEmpty() ? null : value;
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
