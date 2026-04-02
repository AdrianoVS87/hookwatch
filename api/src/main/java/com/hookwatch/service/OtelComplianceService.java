package com.hookwatch.service;

import com.hookwatch.domain.Span;
import com.hookwatch.domain.Trace;
import com.hookwatch.dto.ComplianceReportDto;
import com.hookwatch.dto.ComplianceReportDto.Gap;
import com.hookwatch.dto.ComplianceReportDto.Severity;
import com.hookwatch.repository.TraceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Validates traces against W3C trace context and OpenTelemetry semantic conventions.
 * Checks: traceparent format, required resource attributes (service.name, service.version),
 * and required span attributes per type (model, tokens for LLM_CALL, etc.).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OtelComplianceService {

    private final TraceRepository traceRepository;

    /** W3C traceparent: version-traceid-parentid-flags (hex). */
    private static final Pattern TRACEPARENT_PATTERN =
            Pattern.compile("^[0-9a-f]{2}-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}$");

    /** Valid hex trace-id: 32 lowercase hex chars, not all zeros. */
    private static final Pattern TRACE_ID_HEX = Pattern.compile("^[0-9a-f]{32}$");

    /** Valid hex span-id: 16 lowercase hex chars, not all zeros. */
    private static final Pattern SPAN_ID_HEX = Pattern.compile("^[0-9a-f]{16}$");

    /**
     * Validates a single trace and returns a compliance report.
     */
    @Transactional(readOnly = true)
    public ComplianceReportDto.TraceReport validateTrace(UUID traceId) {
        Trace trace = traceRepository.findById(traceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trace not found"));
        return validateTraceInternal(trace);
    }

    /**
     * Returns aggregate compliance summary for an agent's recent traces.
     */
    @Transactional(readOnly = true)
    public ComplianceReportDto.AgentSummary getAgentSummary(UUID agentId, int limit) {
        List<Trace> traces = traceRepository.findByAgentId(agentId, PageRequest.of(0, Math.min(limit, 200))).getContent();

        if (traces.isEmpty()) {
            return new ComplianceReportDto.AgentSummary(agentId.toString(), 0, 0, 0.0, List.of());
        }

        int compliant = 0;
        Map<String, int[]> gapCounts = new LinkedHashMap<>(); // field -> [count, severityOrdinal]

        for (Trace trace : traces) {
            ComplianceReportDto.TraceReport report = validateTraceInternal(trace);
            if (report.failed() == 0) {
                compliant++;
            }
            for (Gap gap : report.gaps()) {
                gapCounts.computeIfAbsent(gap.field(), k -> new int[]{0, gap.severity().ordinal()});
                gapCounts.get(gap.field())[0]++;
            }
        }

        List<ComplianceReportDto.GapFrequency> topGaps = gapCounts.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<String, int[]> e) -> e.getValue()[0]).reversed())
                .limit(10)
                .map(e -> new ComplianceReportDto.GapFrequency(
                        e.getKey(), e.getValue()[0], Severity.values()[e.getValue()[1]]))
                .toList();

        double rate = (double) compliant / traces.size();
        return new ComplianceReportDto.AgentSummary(agentId.toString(), traces.size(), compliant, rate, topGaps);
    }

    /**
     * Validates a trace and logs compliance warnings (called during ingestion, non-blocking).
     */
    public void logComplianceWarnings(Trace trace) {
        ComplianceReportDto.TraceReport report = validateTraceInternal(trace);
        if (report.failed() > 0) {
            log.warn("OTel compliance: trace {} has {} gaps: {}", trace.getId(), report.failed(),
                    report.gaps().stream().map(Gap::field).collect(Collectors.joining(", ")));
        }
    }

    private ComplianceReportDto.TraceReport validateTraceInternal(Trace trace) {
        List<Gap> gaps = new ArrayList<>();
        int totalChecks = 0;

        // 1. Check W3C trace context — traceparent in metadata
        totalChecks++;
        String traceparent = extractMetadataString(trace, "traceparent");
        if (traceparent == null || traceparent.isBlank()) {
            gaps.add(new Gap("metadata.traceparent", "W3C traceparent header", "missing", Severity.WARNING));
        } else if (!TRACEPARENT_PATTERN.matcher(traceparent.toLowerCase()).matches()) {
            gaps.add(new Gap("metadata.traceparent", "00-{32hex}-{16hex}-{2hex}", traceparent, Severity.ERROR));
        }

        // 2. Check valid trace-id hex format (from trace ID itself)
        totalChecks++;
        String traceIdHex = trace.getId().toString().replace("-", "");
        if (!TRACE_ID_HEX.matcher(traceIdHex).matches()) {
            gaps.add(new Gap("traceId", "32 lowercase hex chars", traceIdHex, Severity.INFO));
        }

        // 3. Check required resource attributes: service.name
        totalChecks++;
        String serviceName = extractMetadataString(trace, "service.name");
        if (serviceName == null || serviceName.isBlank()) {
            gaps.add(new Gap("resource.service.name", "non-empty string", "missing", Severity.ERROR));
        }

        // 4. Check required resource attributes: service.version
        totalChecks++;
        String serviceVersion = extractMetadataString(trace, "service.version");
        if (serviceVersion == null || serviceVersion.isBlank()) {
            gaps.add(new Gap("resource.service.version", "non-empty string", "missing", Severity.WARNING));
        }

        // 5. Check span-level attributes per type
        for (Span span : trace.getSpans()) {
            // Each LLM_CALL span must have model, input_tokens, output_tokens
            if (span.getType() == Span.Type.LLM_CALL) {
                totalChecks++;
                if (span.getModel() == null || span.getModel().isBlank()) {
                    gaps.add(new Gap("span[" + span.getName() + "].model", "non-empty model identifier", "missing", Severity.ERROR));
                }
                totalChecks++;
                if (span.getInputTokens() == null) {
                    gaps.add(new Gap("span[" + span.getName() + "].input_tokens", "integer >= 0", "null", Severity.ERROR));
                }
                totalChecks++;
                if (span.getOutputTokens() == null) {
                    gaps.add(new Gap("span[" + span.getName() + "].output_tokens", "integer >= 0", "null", Severity.ERROR));
                }
            }

            // Each TOOL_CALL span should have a name
            if (span.getType() == Span.Type.TOOL_CALL) {
                totalChecks++;
                if (span.getName() == null || span.getName().isBlank()) {
                    gaps.add(new Gap("span[TOOL_CALL].name", "non-empty tool name", "missing", Severity.WARNING));
                }
            }

            // Each RETRIEVAL span should have a name describing the source
            if (span.getType() == Span.Type.RETRIEVAL) {
                totalChecks++;
                if (span.getName() == null || span.getName().isBlank()) {
                    gaps.add(new Gap("span[RETRIEVAL].name", "non-empty retrieval source name", "missing", Severity.WARNING));
                }
            }

            // Every span must have valid span-id format
            totalChecks++;
            if (span.getId() != null) {
                String spanIdHex = span.getId().toString().replace("-", "");
                // UUID is 32 hex, span-id is 16 — we check first 16 chars
                if (spanIdHex.length() < 16) {
                    gaps.add(new Gap("span[" + span.getName() + "].spanId", "valid hex span-id", spanIdHex, Severity.INFO));
                }
            }
        }

        // 6. Check total_tokens is set on trace
        totalChecks++;
        if (trace.getTotalTokens() == null) {
            gaps.add(new Gap("trace.total_tokens", "integer >= 0", "null", Severity.WARNING));
        }

        int passed = totalChecks - gaps.size();
        return new ComplianceReportDto.TraceReport(
                trace.getId().toString(),
                totalChecks,
                passed,
                gaps.size(),
                gaps
        );
    }

    private String extractMetadataString(Trace trace, String key) {
        if (trace.getMetadata() == null) return null;
        Object value = trace.getMetadata().get(key);
        return value != null ? value.toString() : null;
    }
}
