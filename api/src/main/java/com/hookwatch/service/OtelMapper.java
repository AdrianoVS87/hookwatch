package com.hookwatch.service;

import com.hookwatch.domain.Agent;
import com.hookwatch.domain.Span;
import com.hookwatch.domain.Trace;
import com.hookwatch.dto.OtelExportDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Bidirectional mapper between HookWatch domain objects and OTLP (OpenTelemetry Protocol) format.
 *
 * OTel span kinds:
 *   0 = UNSPECIFIED, 1 = INTERNAL, 2 = SERVER, 3 = CLIENT, 4 = PRODUCER, 5 = CONSUMER
 *
 * Status codes:
 *   0 = UNSET (RUNNING), 1 = OK (COMPLETED), 2 = ERROR (FAILED)
 */
@Component
public class OtelMapper {

    private static final String SCOPE_NAME = "hookwatch";
    private static final String SCOPE_VERSION = "1.0.0";

    // Span kind constants
    static final int KIND_UNSPECIFIED = 0;
    static final int KIND_INTERNAL = 1;
    static final int KIND_CLIENT = 3;

    // Status code constants
    static final int STATUS_UNSET = 0;
    static final int STATUS_OK = 1;
    static final int STATUS_ERROR = 2;

    /**
     * Converts a HookWatch Trace (with spans) and its Agent into an OTLP ExportDto.
     */
    public OtelExportDto toOtel(Trace trace, Agent agent) {
        List<OtelExportDto.OtelSpan> otelSpans = new ArrayList<>();

        for (Span span : trace.getSpans()) {
            otelSpans.add(spanToOtel(trace.getId(), span));
        }

        // Resource attributes: identify the service/agent
        List<OtelExportDto.KeyValue> resourceAttrs = List.of(
                strAttr("service.name", agent.getName()),
                strAttr("service.instance.id", agent.getId().toString()),
                strAttr("hookwatch.agent.id", agent.getId().toString()),
                strAttr("hookwatch.tenant.id", agent.getTenantId().toString())
        );

        OtelExportDto.Resource resource = new OtelExportDto.Resource(resourceAttrs);
        OtelExportDto.InstrumentationScope scope = new OtelExportDto.InstrumentationScope(SCOPE_NAME, SCOPE_VERSION);
        OtelExportDto.ScopeSpans scopeSpans = new OtelExportDto.ScopeSpans(scope, otelSpans);
        OtelExportDto.ResourceSpans resourceSpans = new OtelExportDto.ResourceSpans(resource, List.of(scopeSpans));

        return new OtelExportDto(List.of(resourceSpans));
    }

    /**
     * Converts an OTLP ExportDto into a HookWatch Trace with Spans.
     * Uses the first ResourceSpans/ScopeSpans found. The agentId must be
     * set by the caller after determining the target agent.
     */
    public Trace fromOtel(OtelExportDto dto) {
        if (dto.resourceSpans() == null || dto.resourceSpans().isEmpty()) {
            Trace empty = new Trace();
            empty.setStatus(Trace.Status.RUNNING);
            empty.setSpans(new ArrayList<>());
            return empty;
        }

        OtelExportDto.ResourceSpans resourceSpans = dto.resourceSpans().get(0);
        List<Span> spans = new ArrayList<>();

        // Extract agentId from resource attributes if present
        UUID agentIdFromAttrs = extractAgentIdFromResource(resourceSpans.resource());

        if (resourceSpans.scopeSpans() != null) {
            for (OtelExportDto.ScopeSpans scopeSpans : resourceSpans.scopeSpans()) {
                if (scopeSpans.spans() != null) {
                    for (OtelExportDto.OtelSpan otelSpan : scopeSpans.spans()) {
                        spans.add(otelSpanToHookWatch(otelSpan));
                    }
                }
            }
        }

        // Derive overall trace status from spans
        Trace.Status traceStatus = deriveTraceStatus(spans);

        Trace trace = new Trace();
        trace.setAgentId(agentIdFromAttrs);
        trace.setStatus(traceStatus);
        trace.setSpans(spans);

        // Aggregate totals
        int totalTokens = spans.stream()
                .mapToInt(s -> {
                    int t = 0;
                    if (s.getInputTokens() != null) t += s.getInputTokens();
                    if (s.getOutputTokens() != null) t += s.getOutputTokens();
                    return t;
                }).sum();
        double totalCost = spans.stream()
                .filter(s -> s.getCost() != null)
                .mapToDouble(s -> s.getCost().doubleValue())
                .sum();

        if (totalTokens > 0) trace.setTotalTokens(totalTokens);
        if (totalCost > 0) trace.setTotalCost(BigDecimal.valueOf(totalCost));

        return trace;
    }

    // ---- private helpers ----

    private OtelExportDto.OtelSpan spanToOtel(UUID traceId, Span span) {
        String traceIdHex = uuidToHex(traceId);
        String spanIdHex = uuidToHex(span.getId());
        String parentSpanIdHex = span.getParentSpanId() != null
                ? uuidToHex(span.getParentSpanId()) : null;

        long startNano = toUnixNano(span.getStartedAt());
        long endNano = span.getCompletedAt() != null
                ? toUnixNano(span.getCompletedAt()) : startNano;

        List<OtelExportDto.KeyValue> attrs = buildSpanAttributes(span);

        OtelExportDto.Status status = mapStatus(span.getStatus());
        int kind = mapSpanKind(span.getType());

        return new OtelExportDto.OtelSpan(
                traceIdHex,
                spanIdHex,
                parentSpanIdHex,
                span.getName(),
                kind,
                String.valueOf(startNano),
                String.valueOf(endNano),
                status,
                attrs
        );
    }

    private Span otelSpanToHookWatch(OtelExportDto.OtelSpan otelSpan) {
        Span span = new Span();
        span.setName(otelSpan.name());

        // Try to parse spanId as UUID, otherwise generate one
        span.setId(hexToUuid(otelSpan.spanId()));

        if (otelSpan.parentSpanId() != null && !otelSpan.parentSpanId().isBlank()) {
            span.setParentSpanId(hexToUuid(otelSpan.parentSpanId()));
        }

        // Map kind back to Span.Type
        span.setType(mapKindToType(otelSpan.kind()));

        // Map status code back
        span.setStatus(mapStatusCode(otelSpan.status()));

        // Parse timestamps
        if (otelSpan.startTimeUnixNano() != null) {
            span.setStartedAt(nanoToInstant(otelSpan.startTimeUnixNano()));
        }
        if (otelSpan.endTimeUnixNano() != null) {
            span.setCompletedAt(nanoToInstant(otelSpan.endTimeUnixNano()));
        }

        // Extract attributes
        if (otelSpan.attributes() != null) {
            for (OtelExportDto.KeyValue kv : otelSpan.attributes()) {
                applyAttribute(span, kv);
            }
        }

        return span;
    }

    private List<OtelExportDto.KeyValue> buildSpanAttributes(Span span) {
        List<OtelExportDto.KeyValue> attrs = new ArrayList<>();
        if (span.getType() != null) {
            attrs.add(strAttr("hookwatch.span.type", span.getType().name()));
        }
        if (span.getInputTokens() != null) {
            attrs.add(intAttr("hookwatch.tokens.input", (long) span.getInputTokens()));
        }
        if (span.getOutputTokens() != null) {
            attrs.add(intAttr("hookwatch.tokens.output", (long) span.getOutputTokens()));
        }
        if (span.getCost() != null) {
            // Store cost in micro-cents (multiply by 1_000_000 for precision)
            long costCents = span.getCost().multiply(BigDecimal.valueOf(100)).longValue();
            attrs.add(intAttr("hookwatch.cost.cents", costCents));
        }
        if (span.getModel() != null) {
            attrs.add(strAttr("hookwatch.model", span.getModel()));
        }
        return attrs;
    }

    private void applyAttribute(Span span, OtelExportDto.KeyValue kv) {
        if (kv == null || kv.key() == null || kv.value() == null) return;
        OtelExportDto.Value v = kv.value();
        switch (kv.key()) {
            case "hookwatch.span.type" -> {
                if (v.stringValue() != null) {
                    try {
                        span.setType(Span.Type.valueOf(v.stringValue()));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
            case "hookwatch.tokens.input" -> {
                if (v.intValue() != null) span.setInputTokens(v.intValue().intValue());
            }
            case "hookwatch.tokens.output" -> {
                if (v.intValue() != null) span.setOutputTokens(v.intValue().intValue());
            }
            case "hookwatch.cost.cents" -> {
                if (v.intValue() != null) {
                    span.setCost(BigDecimal.valueOf(v.intValue()).divide(BigDecimal.valueOf(100)));
                }
            }
            case "hookwatch.model" -> {
                if (v.stringValue() != null) span.setModel(v.stringValue());
            }
        }
    }

    /**
     * Maps HookWatch Span.Type to OTel span kind.
     * TOOL_CALL → CLIENT (3), LLM_CALL → INTERNAL (1), RETRIEVAL → CLIENT (3), CUSTOM → INTERNAL (1)
     */
    int mapSpanKind(Span.Type type) {
        if (type == null) return KIND_UNSPECIFIED;
        return switch (type) {
            case TOOL_CALL -> KIND_CLIENT;
            case LLM_CALL -> KIND_INTERNAL;
            case RETRIEVAL -> KIND_CLIENT;
            case CUSTOM -> KIND_INTERNAL;
        };
    }

    /**
     * Maps OTel span kind back to HookWatch Span.Type (best effort).
     */
    private Span.Type mapKindToType(int kind) {
        return switch (kind) {
            case KIND_CLIENT -> Span.Type.TOOL_CALL;
            case KIND_INTERNAL -> Span.Type.LLM_CALL;
            default -> Span.Type.CUSTOM;
        };
    }

    /**
     * Maps HookWatch Span.Status to OTel Status.
     * COMPLETED → OK(1), FAILED → ERROR(2), RUNNING → UNSET(0)
     */
    OtelExportDto.Status mapStatus(Span.Status status) {
        if (status == null) return new OtelExportDto.Status(STATUS_UNSET, "");
        return switch (status) {
            case COMPLETED -> new OtelExportDto.Status(STATUS_OK, "OK");
            case FAILED -> new OtelExportDto.Status(STATUS_ERROR, "ERROR");
            case RUNNING -> new OtelExportDto.Status(STATUS_UNSET, "");
        };
    }

    private Span.Status mapStatusCode(OtelExportDto.Status status) {
        if (status == null) return Span.Status.RUNNING;
        return switch (status.code()) {
            case STATUS_OK -> Span.Status.COMPLETED;
            case STATUS_ERROR -> Span.Status.FAILED;
            default -> Span.Status.RUNNING;
        };
    }

    private Trace.Status deriveTraceStatus(List<Span> spans) {
        if (spans.isEmpty()) return Trace.Status.COMPLETED;
        boolean anyFailed = spans.stream().anyMatch(s -> s.getStatus() == Span.Status.FAILED);
        boolean anyRunning = spans.stream().anyMatch(s -> s.getStatus() == Span.Status.RUNNING);
        if (anyFailed) return Trace.Status.FAILED;
        if (anyRunning) return Trace.Status.RUNNING;
        return Trace.Status.COMPLETED;
    }

    /** Converts a UUID to a 32-character lowercase hex string (OTel standard). */
    static String uuidToHex(UUID uuid) {
        if (uuid == null) return null;
        return uuid.toString().replace("-", "");
    }

    /** Converts a 32-char hex string back to UUID. Falls back to random UUID on parse error. */
    static UUID hexToUuid(String hex) {
        if (hex == null || hex.isBlank()) return UUID.randomUUID();
        try {
            // Insert dashes at standard UUID positions: 8-4-4-4-12
            if (hex.length() == 32) {
                String formatted = hex.substring(0, 8) + "-"
                        + hex.substring(8, 12) + "-"
                        + hex.substring(12, 16) + "-"
                        + hex.substring(16, 20) + "-"
                        + hex.substring(20);
                return UUID.fromString(formatted);
            }
            return UUID.fromString(hex);
        } catch (IllegalArgumentException e) {
            return UUID.randomUUID();
        }
    }

    private long toUnixNano(Instant instant) {
        if (instant == null) return 0L;
        return instant.getEpochSecond() * 1_000_000_000L + instant.getNano();
    }

    private Instant nanoToInstant(String nanoStr) {
        try {
            long nanos = Long.parseLong(nanoStr);
            long seconds = nanos / 1_000_000_000L;
            int remainingNanos = (int) (nanos % 1_000_000_000L);
            return Instant.ofEpochSecond(seconds, remainingNanos);
        } catch (NumberFormatException e) {
            return Instant.now();
        }
    }

    private UUID extractAgentIdFromResource(OtelExportDto.Resource resource) {
        if (resource == null || resource.attributes() == null) return null;
        return resource.attributes().stream()
                .filter(kv -> "hookwatch.agent.id".equals(kv.key()))
                .findFirst()
                .map(kv -> kv.value() != null ? kv.value().stringValue() : null)
                .map(s -> {
                    try { return UUID.fromString(s); } catch (Exception e) { return null; }
                })
                .orElse(null);
    }

    private OtelExportDto.KeyValue strAttr(String key, String value) {
        return new OtelExportDto.KeyValue(key, new OtelExportDto.Value(value, null, null));
    }

    private OtelExportDto.KeyValue intAttr(String key, long value) {
        return new OtelExportDto.KeyValue(key, new OtelExportDto.Value(null, value, null));
    }
}
