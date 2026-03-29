package com.hookwatch.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "OTLP JSON-compatible payload")
public record OtelExportDto(
        @Schema(description = "List of resource spans")
        List<ResourceSpans> resourceSpans
) {

    public record ResourceSpans(
            @Schema(description = "Resource attributes")
            Resource resource,
            @Schema(description = "Spans grouped by instrumentation scope")
            List<ScopeSpans> scopeSpans
    ) {}

    public record Resource(@Schema(description = "Resource key/value attributes") List<KeyValue> attributes) {}

    public record ScopeSpans(
            @Schema(description = "Instrumentation scope metadata")
            InstrumentationScope scope,
            @Schema(description = "Spans in this scope")
            List<OtelSpan> spans
    ) {}

    public record InstrumentationScope(
            @Schema(description = "Instrumentation scope name", example = "hookwatch-exporter")
            String name,
            @Schema(description = "Instrumentation scope version", example = "1.0.0")
            String version
    ) {}

    public record OtelSpan(
            @Schema(description = "Hex trace id", example = "4bf92f3577b34da6a3ce929d0e0e4736")
            String traceId,
            @Schema(description = "Hex span id", example = "00f067aa0ba902b7")
            String spanId,
            @Schema(description = "Hex parent span id", example = "3a2fb9f4d4c8321a")
            String parentSpanId,
            @Schema(description = "Span display name", example = "llm.completion")
            String name,
            @Schema(description = "Span kind code", example = "1")
            int kind,
            @Schema(description = "Start time (unix nanos)", example = "1711548150000000000")
            String startTimeUnixNano,
            @Schema(description = "End time (unix nanos)", example = "1711548153000000000")
            String endTimeUnixNano,
            @Schema(description = "OTEL status")
            Status status,
            @Schema(description = "Span attributes")
            List<KeyValue> attributes
    ) {}

    public record Status(
            @Schema(description = "Status code", example = "1")
            int code,
            @Schema(description = "Status message", example = "OK")
            String message
    ) {}

    public record KeyValue(
            @Schema(description = "Attribute key", example = "hookwatch.agent.id")
            String key,
            @Schema(description = "Attribute value")
            Value value
    ) {}

    public record Value(
            @Schema(description = "String value", example = "OpenClaw Assistant")
            String stringValue,
            @Schema(description = "Integer value", example = "42")
            Long intValue,
            @Schema(description = "Double value", example = "0.014")
            Double doubleValue
    ) {}
}
