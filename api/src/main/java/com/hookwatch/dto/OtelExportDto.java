package com.hookwatch.dto;

import java.util.List;

public record OtelExportDto(List<ResourceSpans> resourceSpans) {

    public record ResourceSpans(Resource resource, List<ScopeSpans> scopeSpans) {}

    public record Resource(List<KeyValue> attributes) {}

    public record ScopeSpans(InstrumentationScope scope, List<OtelSpan> spans) {}

    public record InstrumentationScope(String name, String version) {}

    public record OtelSpan(
            String traceId,
            String spanId,
            String parentSpanId,
            String name,
            int kind,
            String startTimeUnixNano,
            String endTimeUnixNano,
            Status status,
            List<KeyValue> attributes
    ) {}

    public record Status(int code, String message) {}

    public record KeyValue(String key, Value value) {}

    public record Value(String stringValue, Long intValue, Double doubleValue) {}
}
