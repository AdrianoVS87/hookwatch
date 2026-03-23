package com.hookwatch.service;

import com.hookwatch.domain.Span;
import com.hookwatch.domain.Trace;
import com.hookwatch.dto.SpanDto;
import com.hookwatch.dto.TraceDto;
import com.hookwatch.repository.TraceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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

        Trace saved = traceRepository.save(trace);

        if (dto.getSpans() != null && !dto.getSpans().isEmpty()) {
            List<Span> spans = dto.getSpans().stream()
                    .map(spanDto -> {
                        Span span = mapSpan(spanDto);
                        span.setTraceId(saved.getId());
                        return span;
                    })
                    .toList();
            saved.setSpans(spans);
            traceRepository.save(saved);
        }

        Trace result = traceRepository.findById(saved.getId()).orElse(saved);
        eventPublisher.publish(result.getId(), result);
        return result;
    }

    public Page<Trace> findByAgentId(UUID agentId, Pageable pageable) {
        return traceRepository.findByAgentId(agentId, pageable);
    }

    public Optional<Trace> findById(UUID id) {
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
